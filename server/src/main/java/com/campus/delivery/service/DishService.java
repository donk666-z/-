package com.campus.delivery.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.delivery.entity.Dish;
import com.campus.delivery.model.ComboConfig;

import java.util.List;

public interface DishService extends IService<Dish> {
    String TYPE_SINGLE = "single";
    String TYPE_COMBO = "combo";

    ComboConfig parseComboConfig(Dish dish);

    String writeComboConfig(ComboConfig comboConfig);

    ComboConfig normalizeComboConfig(Long merchantId, ComboConfig comboConfig, Long currentDishId);

    Dish enrichDish(Dish dish);

    List<Dish> enrichDishes(List<Dish> dishes);

    void ensureDishNotReferencedByCombo(Long merchantId, Long dishId, Long excludeDishId);
}
