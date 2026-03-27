package com.campus.delivery.controller.student;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.delivery.common.Result;
import com.campus.delivery.dto.CategoryDishesVO;
import com.campus.delivery.entity.Category;
import com.campus.delivery.entity.Dish;
import com.campus.delivery.entity.Merchant;
import com.campus.delivery.service.CategoryService;
import com.campus.delivery.service.DishService;
import com.campus.delivery.service.MerchantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/student/merchant")
public class StudentMerchantController {

    @Autowired
    private MerchantService merchantService;

    @Autowired
    private DishService dishService;

    @Autowired
    private CategoryService categoryService;

    @GetMapping("/list")
    public Result<List<Merchant>> getList(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String status
    ) {
        List<Merchant> list = merchantService.getList(keyword, status);
        return Result.success(list);
    }

    @GetMapping("/{id}")
    public Result<Merchant> getDetail(@PathVariable Long id) {
        Merchant merchant = merchantService.getDetail(id);
        return Result.success(merchant);
    }

    @GetMapping("/{id}/dishes")
    public Result<List<CategoryDishesVO>> getDishes(@PathVariable Long id) {
        LambdaQueryWrapper<Category> categoryWrapper = new LambdaQueryWrapper<>();
        categoryWrapper.eq(Category::getMerchantId, id)
                      .orderByAsc(Category::getSort);
        List<Category> categories = categoryService.list(categoryWrapper);

        LambdaQueryWrapper<Dish> dishWrapper = new LambdaQueryWrapper<>();
        dishWrapper.eq(Dish::getMerchantId, id)
                  .eq(Dish::getStatus, "available");
        List<Dish> allDishes = dishService.list(dishWrapper);
        dishService.enrichDishes(allDishes);

        Map<Long, List<Dish>> dishesByCategory = allDishes.stream()
                .collect(Collectors.groupingBy(Dish::getCategoryId));

        List<CategoryDishesVO> result = new ArrayList<>();
        for (Category category : categories) {
            CategoryDishesVO vo = new CategoryDishesVO();
            vo.setId(category.getId());
            vo.setName(category.getName());
            vo.setSort(category.getSort());
            vo.setDishes(dishesByCategory.getOrDefault(category.getId(), new ArrayList<>()));
            result.add(vo);
        }

        return Result.success(result);
    }
}
