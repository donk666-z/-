package com.campus.delivery.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.delivery.entity.Category;
import com.campus.delivery.entity.Dish;
import com.campus.delivery.mapper.DishMapper;
import com.campus.delivery.model.ComboConfig;
import com.campus.delivery.service.CategoryService;
import com.campus.delivery.service.DishService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int DEFAULT_COMBO_STOCK = 999;

    private final CategoryService categoryService;

    public DishServiceImpl(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @Override
    public ComboConfig parseComboConfig(Dish dish) {
        if (dish == null || !StringUtils.hasText(dish.getComboConfigJson())) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(dish.getComboConfigJson(), ComboConfig.class);
        } catch (Exception ex) {
            throw new IllegalArgumentException("套餐配置解析失败");
        }
    }

    @Override
    public String writeComboConfig(ComboConfig comboConfig) {
        if (comboConfig == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(comboConfig);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("套餐配置保存失败");
        }
    }

    @Override
    public ComboConfig normalizeComboConfig(Long merchantId, ComboConfig comboConfig, Long currentDishId) {
        if (comboConfig == null || comboConfig.getRules() == null || comboConfig.getRules().isEmpty()) {
            throw new IllegalArgumentException("套餐至少需要一个分类规则");
        }

        Set<Long> categoryIds = comboConfig.getRules().stream()
                .filter(Objects::nonNull)
                .map(ComboConfig.Rule::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (categoryIds.isEmpty()) {
            throw new IllegalArgumentException("套餐分类规则不能为空");
        }

        Map<Long, Category> categoryMap = categoryService.listByIds(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, item -> item, (left, right) -> left));

        ComboConfig normalized = new ComboConfig();
        List<ComboConfig.Rule> rules = new ArrayList<ComboConfig.Rule>();
        for (ComboConfig.Rule rawRule : comboConfig.getRules()) {
            if (rawRule == null) {
                continue;
            }

            Long categoryId = rawRule.getCategoryId();
            if (categoryId == null) {
                throw new IllegalArgumentException("套餐分类不能为空");
            }

            Category category = categoryMap.get(categoryId);
            if (category == null || !merchantId.equals(category.getMerchantId())) {
                throw new IllegalArgumentException("套餐分类不存在或不属于当前商家");
            }

            if (rules.stream().anyMatch(rule -> categoryId.equals(rule.getCategoryId()))) {
                throw new IllegalArgumentException("同一个套餐内分类不能重复");
            }

            int requiredCount = rawRule.getRequiredCount() == null ? 1 : rawRule.getRequiredCount();
            if (requiredCount <= 0) {
                throw new IllegalArgumentException("套餐分类选择数量必须大于 0");
            }

            LambdaQueryWrapper<Dish> wrapper = new LambdaQueryWrapper<Dish>();
            wrapper.eq(Dish::getMerchantId, merchantId)
                    .eq(Dish::getCategoryId, categoryId)
                    .eq(Dish::getType, TYPE_SINGLE);
            if (currentDishId != null) {
                wrapper.ne(Dish::getId, currentDishId);
            }

            List<Dish> categoryDishes = list(wrapper);
            long singleDishCount = categoryDishes.stream()
                    .filter(dish -> TYPE_SINGLE.equals(normalizeDishType(dish.getType())))
                    .count();
            if (singleDishCount < requiredCount) {
                throw new IllegalArgumentException("套餐分类“" + category.getName() + "”下可选单品不足");
            }

            ComboConfig.Rule rule = new ComboConfig.Rule();
            rule.setCategoryId(categoryId);
            rule.setCategoryName(category.getName());
            rule.setRequiredCount(requiredCount);
            rules.add(rule);
        }

        normalized.setRules(rules);
        return normalized;
    }

    @Override
    public Dish enrichDish(Dish dish) {
        if (dish == null) {
            return null;
        }
        return enrichDishes(Collections.singletonList(dish)).get(0);
    }

    @Override
    public List<Dish> enrichDishes(List<Dish> dishes) {
        if (dishes == null || dishes.isEmpty()) {
            return dishes;
        }

        Map<Long, ComboConfig> comboConfigMap = new HashMap<Long, ComboConfig>();
        Set<Long> categoryIds = dishes.stream()
                .peek(dish -> {
                    dish.setType(normalizeDishType(dish.getType()));
                    if (dish.getStock() == null) {
                        dish.setStock(0);
                    }
                })
                .filter(dish -> TYPE_COMBO.equals(dish.getType()))
                .map(dish -> {
                    ComboConfig comboConfig = parseComboConfig(dish);
                    if (comboConfig == null) {
                        comboConfig = new ComboConfig();
                        comboConfig.setRules(new ArrayList<ComboConfig.Rule>());
                    }
                    comboConfigMap.put(dish.getId(), comboConfig);
                    return comboConfig;
                })
                .flatMap(comboConfig -> safeRules(comboConfig).stream())
                .map(ComboConfig.Rule::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, List<Dish>> dishesByCategory;
        if (categoryIds.isEmpty()) {
            dishesByCategory = Collections.emptyMap();
        } else {
            LambdaQueryWrapper<Dish> wrapper = new LambdaQueryWrapper<Dish>();
            wrapper.in(Dish::getCategoryId, categoryIds)
                    .eq(Dish::getType, TYPE_SINGLE);
            dishesByCategory = list(wrapper).stream()
                    .collect(Collectors.groupingBy(Dish::getCategoryId));
        }

        for (Dish dish : dishes) {
            if (!TYPE_COMBO.equals(dish.getType())) {
                dish.setComboConfig(null);
                continue;
            }

            ComboConfig comboConfig = comboConfigMap.get(dish.getId());
            enrichComboConfig(dish.getMerchantId(), comboConfig, dishesByCategory);
            dish.setComboConfig(comboConfig);
            dish.setStock(calculateComboDisplayStock(comboConfig));
        }
        return dishes;
    }

    @Override
    public void ensureDishNotReferencedByCombo(Long merchantId, Long dishId, Long excludeDishId) {
        // 套餐改为按分类选品后，不再直接引用单品。
    }

    private void enrichComboConfig(Long merchantId, ComboConfig comboConfig, Map<Long, List<Dish>> dishesByCategory) {
        if (comboConfig == null) {
            return;
        }

        for (ComboConfig.Rule rule : safeRules(comboConfig)) {
            List<Dish> categoryDishes = dishesByCategory.getOrDefault(rule.getCategoryId(), Collections.<Dish>emptyList());
            List<ComboConfig.Item> items = categoryDishes.stream()
                    .filter(item -> merchantId.equals(item.getMerchantId()))
                    .filter(item -> TYPE_SINGLE.equals(normalizeDishType(item.getType())))
                    .map(item -> {
                        ComboConfig.Item comboItem = new ComboConfig.Item();
                        comboItem.setDishId(item.getId());
                        comboItem.setDishName(item.getName());
                        comboItem.setDishImage(item.getImage());
                        comboItem.setDishPrice(item.getPrice());
                        comboItem.setDishStock(item.getStock() == null ? 0 : item.getStock());
                        comboItem.setDishStatus(item.getStatus());
                        comboItem.setDishType(normalizeDishType(item.getType()));
                        comboItem.setAvailable("available".equals(item.getStatus()) && (item.getStock() == null || item.getStock() > 0));
                        return comboItem;
                    })
                    .collect(Collectors.toList());
            rule.setItems(items);
            if (!StringUtils.hasText(rule.getCategoryName())) {
                Category category = categoryService.getById(rule.getCategoryId());
                rule.setCategoryName(category == null ? "" : category.getName());
            }
        }
    }

    private int calculateComboDisplayStock(ComboConfig comboConfig) {
        if (comboConfig == null || comboConfig.getRules() == null || comboConfig.getRules().isEmpty()) {
            return DEFAULT_COMBO_STOCK;
        }

        Integer finalStock = null;
        for (ComboConfig.Rule rule : comboConfig.getRules()) {
            int ruleStock = calculateRuleDisplayStock(rule);
            finalStock = finalStock == null ? ruleStock : Math.min(finalStock, ruleStock);
        }
        return finalStock == null ? DEFAULT_COMBO_STOCK : Math.max(finalStock, 0);
    }

    private int calculateRuleDisplayStock(ComboConfig.Rule rule) {
        if (rule == null) {
            return 0;
        }

        int requiredCount = rule.getRequiredCount() == null ? 1 : rule.getRequiredCount();
        if (requiredCount <= 0) {
            return DEFAULT_COMBO_STOCK;
        }

        int totalCapacity = 0;
        int selectableCount = 0;
        for (ComboConfig.Item item : safeItems(rule)) {
            if (!Boolean.TRUE.equals(item.getAvailable())) {
                continue;
            }
            int stock = item.getDishStock() == null ? 0 : item.getDishStock();
            if (stock <= 0) {
                continue;
            }
            selectableCount++;
            totalCapacity += stock;
        }

        if (selectableCount < requiredCount) {
            return 0;
        }
        return Math.max(totalCapacity / requiredCount, 0);
    }

    private List<ComboConfig.Rule> safeRules(ComboConfig comboConfig) {
        return comboConfig == null || comboConfig.getRules() == null
                ? Collections.<ComboConfig.Rule>emptyList()
                : comboConfig.getRules();
    }

    private List<ComboConfig.Item> safeItems(ComboConfig.Rule rule) {
        return rule == null || rule.getItems() == null
                ? Collections.<ComboConfig.Item>emptyList()
                : rule.getItems();
    }

    private String normalizeDishType(String rawType) {
        if (TYPE_COMBO.equalsIgnoreCase(rawType)) {
            return TYPE_COMBO;
        }
        return TYPE_SINGLE;
    }
}
