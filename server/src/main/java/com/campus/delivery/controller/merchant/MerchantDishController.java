package com.campus.delivery.controller.merchant;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.delivery.common.Result;
import com.campus.delivery.entity.Category;
import com.campus.delivery.entity.Dish;
import com.campus.delivery.entity.Merchant;
import com.campus.delivery.service.CategoryService;
import com.campus.delivery.service.DishService;
import com.campus.delivery.service.MerchantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/merchant/dish")
public class MerchantDishController {

    @Autowired
    private DishService dishService;

    @Autowired
    private MerchantService merchantService;

    @Autowired
    private CategoryService categoryService;

    @GetMapping("/list")
    public Result<List<Dish>> getList(HttpServletRequest request) {
        Merchant merchant = requireMerchant(request);
        if (merchant == null) {
            return Result.error("\u5546\u6237\u4e0d\u5b58\u5728");
        }

        LambdaQueryWrapper<Dish> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Dish::getMerchantId, merchant.getId())
                .orderByDesc(Dish::getUpdatedAt)
                .orderByDesc(Dish::getId);
        List<Dish> dishes = dishService.list(wrapper);
        fillCategoryNames(dishes);
        dishService.enrichDishes(dishes);
        return Result.success(dishes);
    }

    @GetMapping("/{id}")
    public Result<Dish> getDetail(@PathVariable Long id, HttpServletRequest request) {
        Merchant merchant = requireMerchant(request);
        if (merchant == null) {
            return Result.error("\u5546\u6237\u4e0d\u5b58\u5728");
        }

        Dish dish = dishService.getById(id);
        if (dish == null || !merchant.getId().equals(dish.getMerchantId())) {
            return Result.error("\u83dc\u54c1\u4e0d\u5b58\u5728");
        }

        fillCategoryNames(Collections.singletonList(dish));
        dishService.enrichDish(dish);
        return Result.success(dish);
    }

    @PostMapping
    public Result<Dish> create(@RequestBody Dish dish, HttpServletRequest request) {
        Merchant merchant = requireMerchant(request);
        if (merchant == null) {
            return Result.error("\u5546\u6237\u4e0d\u5b58\u5728");
        }

        try {
            Category category = categoryService.resolveCategory(merchant.getId(), dish.getCategoryId(), dish.getCategory());
            dish.setMerchantId(merchant.getId());
            dish.setCategoryId(category.getId());
            dish.setStatus(normalizeDishStatus(dish.getStatus(), "available"));
            normalizeDishPayload(merchant.getId(), dish, null);
            dishService.save(dish);
            dishService.enrichDish(dish);
            dish.setCategory(category.getName());
            return Result.success("\u83dc\u54c1\u521b\u5efa\u6210\u529f", dish);
        } catch (IllegalArgumentException ex) {
            return Result.error(ex.getMessage());
        }
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Dish dish, HttpServletRequest request) {
        Merchant merchant = requireMerchant(request);
        if (merchant == null) {
            return Result.error("\u5546\u6237\u4e0d\u5b58\u5728");
        }

        Dish existing = dishService.getById(id);
        if (existing == null || !merchant.getId().equals(existing.getMerchantId())) {
            return Result.error("\u83dc\u54c1\u4e0d\u5b58\u5728\u6216\u65e0\u6743\u9650\u64cd\u4f5c");
        }

        try {
            dish.setId(id);
            dish.setMerchantId(existing.getMerchantId());
            if (dish.getCategoryId() != null || StringUtils.hasText(dish.getCategory())) {
                Category category = categoryService.resolveCategory(merchant.getId(), dish.getCategoryId(), dish.getCategory());
                dish.setCategoryId(category.getId());
            } else {
                dish.setCategoryId(existing.getCategoryId());
            }
            dish.setStatus(normalizeDishStatus(dish.getStatus(), existing.getStatus()));
            normalizeDishPayload(merchant.getId(), dish, id);
            dishService.updateById(dish);
            return Result.success("\u83dc\u54c1\u66f4\u65b0\u6210\u529f", null);
        } catch (IllegalArgumentException ex) {
            return Result.error(ex.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        Merchant merchant = requireMerchant(request);
        if (merchant == null) {
            return Result.error("\u5546\u6237\u4e0d\u5b58\u5728");
        }

        Dish existing = dishService.getById(id);
        if (existing == null || !merchant.getId().equals(existing.getMerchantId())) {
            return Result.error("\u83dc\u54c1\u4e0d\u5b58\u5728\u6216\u65e0\u6743\u9650\u64cd\u4f5c");
        }

        if (!DishService.TYPE_COMBO.equals(existing.getType())) {
            dishService.ensureDishNotReferencedByCombo(merchant.getId(), id, null);
        }
        dishService.removeById(id);
        return Result.success("\u83dc\u54c1\u5220\u9664\u6210\u529f", null);
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, Object> params, HttpServletRequest request) {
        Merchant merchant = requireMerchant(request);
        if (merchant == null) {
            return Result.error("\u5546\u6237\u4e0d\u5b58\u5728");
        }

        Dish existing = dishService.getById(id);
        if (existing == null || !merchant.getId().equals(existing.getMerchantId())) {
            return Result.error("\u83dc\u54c1\u4e0d\u5b58\u5728\u6216\u65e0\u6743\u9650\u64cd\u4f5c");
        }

        String status = normalizeDishStatus(params.get("status"));
        if (status == null) {
            return Result.error("\u83dc\u54c1\u72b6\u6001\u4e0d\u5408\u6cd5");
        }

        Dish dish = new Dish();
        dish.setId(id);
        dish.setStatus(status);
        dishService.updateById(dish);
        return Result.success("\u72b6\u6001\u66f4\u65b0\u6210\u529f", null);
    }

    private void normalizeDishPayload(Long merchantId, Dish dish, Long currentDishId) {
        String type = normalizeDishType(dish.getType(), DishService.TYPE_SINGLE);
        dish.setType(type);
        if (DishService.TYPE_COMBO.equals(type)) {
            if (currentDishId != null) {
                dishService.ensureDishNotReferencedByCombo(merchantId, currentDishId, currentDishId);
            }
            dish.setStock(0);
            dish.setComboConfig(dishService.normalizeComboConfig(merchantId, dish.getComboConfig(), currentDishId));
            dish.setComboConfigJson(dishService.writeComboConfig(dish.getComboConfig()));
        } else {
            dish.setComboConfig(null);
            dish.setComboConfigJson(null);
        }
    }

    private Merchant requireMerchant(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return null;
        }
        return merchantService.getByUserId(userId);
    }

    private void fillCategoryNames(List<Dish> dishes) {
        if (dishes == null || dishes.isEmpty()) {
            return;
        }

        Set<Long> categoryIds = dishes.stream()
                .map(Dish::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (categoryIds.isEmpty()) {
            return;
        }

        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Category::getId, categoryIds);
        Map<Long, String> categoryNameMap = categoryService.list(wrapper).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName, (left, right) -> left));

        dishes.forEach(dish -> dish.setCategory(categoryNameMap.get(dish.getCategoryId())));
    }

    private String normalizeDishStatus(Object rawStatus) {
        if (rawStatus == null) {
            return null;
        }

        String value = String.valueOf(rawStatus).trim();
        if (!StringUtils.hasText(value)) {
            return null;
        }

        if ("1".equals(value) || "true".equalsIgnoreCase(value) || "available".equalsIgnoreCase(value)) {
            return "available";
        }
        if ("0".equals(value) || "false".equalsIgnoreCase(value) || "unavailable".equalsIgnoreCase(value)) {
            return "unavailable";
        }
        return null;
    }

    private String normalizeDishStatus(Object rawStatus, String fallback) {
        String normalized = normalizeDishStatus(rawStatus);
        return normalized != null ? normalized : fallback;
    }

    private String normalizeDishType(Object rawType, String fallback) {
        if (rawType == null) {
            return fallback;
        }

        String value = String.valueOf(rawType).trim();
        if (!StringUtils.hasText(value)) {
            return fallback;
        }

        if (DishService.TYPE_COMBO.equalsIgnoreCase(value)) {
            return DishService.TYPE_COMBO;
        }
        return DishService.TYPE_SINGLE;
    }
}
