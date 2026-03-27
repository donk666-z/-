package com.campus.delivery.dto;

import com.campus.delivery.entity.Dish;
import lombok.Data;
import java.util.List;

@Data
public class CategoryDishesVO {
    private Long id;
    private String name;
    private Integer sort;
    private List<Dish> dishes;
}