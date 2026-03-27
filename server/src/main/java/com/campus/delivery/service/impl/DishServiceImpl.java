package com.campus.delivery.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.delivery.entity.Dish;
import com.campus.delivery.mapper.DishMapper;
import com.campus.delivery.model.ComboConfig;
import com.campus.delivery.service.DishService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int DEFAULT_COMBO_STOCK = 999;

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
        if (comboConfig == null || comboConfig.getGroups() == null || comboConfig.getGroups().isEmpty()) {
            throw new IllegalArgumentException("套餐至少需要一个分组");
        }

        Set<Long> optionDishIds = new HashSet<>();
        for (ComboConfig.Group group : comboConfig.getGroups()) {
            if (group == null) {
                continue;
            }
            if (group.getOptions() != null) {
                for (ComboConfig.Option option : group.getOptions()) {
                    if (option != null && option.getDishId() != null) {
                        optionDishIds.add(option.getDishId());
                    }
                }
            }
        }
        if (optionDishIds.isEmpty()) {
            throw new IllegalArgumentException("套餐选项不能为空");
        }

        Map<Long, Dish> optionDishMap = listByIds(optionDishIds).stream()
                .collect(Collectors.toMap(Dish::getId, dish -> dish, (left, right) -> left));

        ComboConfig normalized = new ComboConfig();
        List<ComboConfig.Group> groups = new ArrayList<>();
        for (ComboConfig.Group rawGroup : comboConfig.getGroups()) {
            if (rawGroup == null) {
                continue;
            }

            String groupName = rawGroup.getName() == null ? null : rawGroup.getName().trim();
            if (!StringUtils.hasText(groupName)) {
                throw new IllegalArgumentException("套餐分组名称不能为空");
            }

            int minSelect = rawGroup.getMinSelect() == null ? 0 : rawGroup.getMinSelect();
            int maxSelect = rawGroup.getMaxSelect() == null ? 0 : rawGroup.getMaxSelect();
            if (minSelect < 0) {
                throw new IllegalArgumentException("套餐最少可选数量不能小于 0");
            }
            if (maxSelect <= 0) {
                throw new IllegalArgumentException("套餐最多可选数量必须大于 0");
            }
            if (maxSelect < minSelect) {
                throw new IllegalArgumentException("套餐分组最多可选数量不能小于最少可选数量");
            }

            if (rawGroup.getOptions() == null || rawGroup.getOptions().isEmpty()) {
                throw new IllegalArgumentException("套餐分组“" + groupName + "”至少需要一个选项");
            }

            Set<Long> seenDishIds = new HashSet<>();
            List<ComboConfig.Option> normalizedOptions = new ArrayList<>();
            for (ComboConfig.Option rawOption : rawGroup.getOptions()) {
                if (rawOption == null || rawOption.getDishId() == null) {
                    throw new IllegalArgumentException("套餐选项缺少菜品");
                }
                if (!seenDishIds.add(rawOption.getDishId())) {
                    throw new IllegalArgumentException("套餐分组“" + groupName + "”存在重复菜品");
                }

                Dish optionDish = optionDishMap.get(rawOption.getDishId());
                if (optionDish == null || !merchantId.equals(optionDish.getMerchantId())) {
                    throw new IllegalArgumentException("套餐选项菜品不存在或不属于当前商家");
                }
                if (currentDishId != null && currentDishId.equals(optionDish.getId())) {
                    throw new IllegalArgumentException("套餐不能引用自身");
                }
                String optionDishType = normalizeDishType(optionDish.getType());
                if (TYPE_COMBO.equals(optionDishType)) {
                    throw new IllegalArgumentException("套餐选项只能选择单品");
                }

                int quantity = rawOption.getQuantity() == null ? 1 : rawOption.getQuantity();
                if (quantity <= 0) {
                    throw new IllegalArgumentException("套餐选项数量必须大于 0");
                }

                BigDecimal extraPrice = rawOption.getExtraPrice() == null ? BigDecimal.ZERO : rawOption.getExtraPrice();
                if (extraPrice.compareTo(BigDecimal.ZERO) < 0) {
                    throw new IllegalArgumentException("套餐加价不能小于 0");
                }

                ComboConfig.Option option = new ComboConfig.Option();
                option.setDishId(optionDish.getId());
                option.setQuantity(quantity);
                option.setExtraPrice(extraPrice);
                normalizedOptions.add(option);
            }

            if (minSelect > normalizedOptions.size()) {
                throw new IllegalArgumentException("套餐分组“" + groupName + "”的最少选择数量超过了可选项数量");
            }

            ComboConfig.Group group = new ComboConfig.Group();
            group.setName(groupName);
            group.setMinSelect(minSelect);
            group.setMaxSelect(maxSelect);
            group.setOptions(normalizedOptions);
            groups.add(group);
        }

        if (groups.isEmpty()) {
            throw new IllegalArgumentException("套餐至少需要一个有效分组");
        }
        normalized.setGroups(groups);
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

        Map<Long, ComboConfig> comboConfigMap = new HashMap<>();
        Set<Long> optionDishIds = new HashSet<>();

        for (Dish dish : dishes) {
            dish.setType(normalizeDishType(dish.getType()));
            if (dish.getStock() == null) {
                dish.setStock(0);
            }
            if (!TYPE_COMBO.equals(dish.getType())) {
                dish.setComboConfig(null);
                continue;
            }

            ComboConfig comboConfig = parseComboConfig(dish);
            if (comboConfig == null) {
                comboConfig = new ComboConfig();
                comboConfig.setGroups(new ArrayList<>());
            }
            comboConfigMap.put(dish.getId(), comboConfig);
            for (ComboConfig.Group group : safeGroups(comboConfig)) {
                for (ComboConfig.Option option : safeOptions(group)) {
                    if (option.getDishId() != null) {
                        optionDishIds.add(option.getDishId());
                    }
                }
            }
        }

        Map<Long, Dish> optionDishMap = optionDishIds.isEmpty()
                ? Collections.emptyMap()
                : listByIds(optionDishIds).stream()
                .collect(Collectors.toMap(Dish::getId, candidate -> candidate, (left, right) -> left));

        for (Dish dish : dishes) {
            if (!TYPE_COMBO.equals(dish.getType())) {
                continue;
            }
            ComboConfig comboConfig = comboConfigMap.get(dish.getId());
            enrichComboConfig(comboConfig, optionDishMap);
            dish.setComboConfig(comboConfig);
            dish.setStock(calculateComboDisplayStock(comboConfig));
        }
        return dishes;
    }

    @Override
    public void ensureDishNotReferencedByCombo(Long merchantId, Long dishId, Long excludeDishId) {
        if (merchantId == null || dishId == null) {
            return;
        }

        LambdaQueryWrapper<Dish> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Dish::getMerchantId, merchantId)
                .eq(Dish::getType, TYPE_COMBO);
        if (excludeDishId != null) {
            wrapper.ne(Dish::getId, excludeDishId);
        }

        for (Dish comboDish : list(wrapper)) {
            ComboConfig comboConfig = parseComboConfig(comboDish);
            for (ComboConfig.Group group : safeGroups(comboConfig)) {
                for (ComboConfig.Option option : safeOptions(group)) {
                    if (dishId.equals(option.getDishId())) {
                        throw new IllegalArgumentException("该单品已被套餐引用，请先调整套餐配置");
                    }
                }
            }
        }
    }

    private void enrichComboConfig(ComboConfig comboConfig, Map<Long, Dish> optionDishMap) {
        if (comboConfig == null) {
            return;
        }
        for (ComboConfig.Group group : safeGroups(comboConfig)) {
            for (ComboConfig.Option option : safeOptions(group)) {
                Dish optionDish = optionDishMap.get(option.getDishId());
                if (optionDish == null) {
                    option.setAvailable(false);
                    continue;
                }
                option.setDishName(optionDish.getName());
                option.setDishImage(optionDish.getImage());
                option.setDishPrice(optionDish.getPrice());
                option.setDishStock(optionDish.getStock());
                option.setDishStatus(optionDish.getStatus());
                option.setDishType(normalizeDishType(optionDish.getType()));
                option.setAvailable(isOptionAvailable(optionDish, option));
            }
        }
    }

    private boolean isOptionAvailable(Dish optionDish, ComboConfig.Option option) {
        if (optionDish == null || option == null) {
            return false;
        }
        if (!TYPE_SINGLE.equals(normalizeDishType(optionDish.getType()))) {
            return false;
        }
        if (!"available".equals(optionDish.getStatus())) {
            return false;
        }
        int required = option.getQuantity() == null || option.getQuantity() <= 0 ? 1 : option.getQuantity();
        int stock = optionDish.getStock() == null ? 0 : optionDish.getStock();
        return stock >= required;
    }

    private int calculateComboDisplayStock(ComboConfig comboConfig) {
        if (comboConfig == null || comboConfig.getGroups() == null || comboConfig.getGroups().isEmpty()) {
            return 0;
        }

        Integer finalStock = null;
        boolean hasRequiredGroup = false;
        for (ComboConfig.Group group : comboConfig.getGroups()) {
            int minSelect = group.getMinSelect() == null ? 0 : group.getMinSelect();
            int groupStock = calculateGroupDisplayStock(group);
            if (minSelect > 0) {
                hasRequiredGroup = true;
                finalStock = finalStock == null ? groupStock : Math.min(finalStock, groupStock);
            }
        }

        if (!hasRequiredGroup) {
            return DEFAULT_COMBO_STOCK;
        }
        if (finalStock == null || finalStock <= 0) {
            return 0;
        }
        return Math.min(finalStock, DEFAULT_COMBO_STOCK);
    }

    private int calculateGroupDisplayStock(ComboConfig.Group group) {
        if (group == null) {
            return 0;
        }
        int minSelect = group.getMinSelect() == null ? 0 : group.getMinSelect();
        if (minSelect <= 0) {
            return DEFAULT_COMBO_STOCK;
        }

        int selectableOptionCount = 0;
        int totalCapacity = 0;
        for (ComboConfig.Option option : safeOptions(group)) {
            if (!Boolean.TRUE.equals(option.getAvailable())) {
                continue;
            }
            int quantity = option.getQuantity() == null || option.getQuantity() <= 0 ? 1 : option.getQuantity();
            int stock = option.getDishStock() == null ? 0 : option.getDishStock();
            int capacity = stock / quantity;
            if (capacity > 0) {
                selectableOptionCount++;
                totalCapacity += capacity;
            }
        }

        if (selectableOptionCount < minSelect) {
            return 0;
        }

        int estimatedStock = totalCapacity / minSelect;
        return Math.max(estimatedStock, 1);
    }

    private List<ComboConfig.Group> safeGroups(ComboConfig comboConfig) {
        return comboConfig == null || comboConfig.getGroups() == null
                ? Collections.emptyList()
                : comboConfig.getGroups();
    }

    private List<ComboConfig.Option> safeOptions(ComboConfig.Group group) {
        return group == null || group.getOptions() == null
                ? Collections.emptyList()
                : group.getOptions();
    }

    private String normalizeDishType(String rawType) {
        if (!StringUtils.hasText(rawType)) {
            return TYPE_SINGLE;
        }
        String normalized = rawType.trim().toLowerCase(Locale.ROOT);
        return TYPE_COMBO.equals(normalized) ? TYPE_COMBO : TYPE_SINGLE;
    }
}
