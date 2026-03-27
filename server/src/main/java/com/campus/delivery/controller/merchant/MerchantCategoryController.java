package com.campus.delivery.controller.merchant;

import com.campus.delivery.common.Result;
import com.campus.delivery.entity.Category;
import com.campus.delivery.entity.Merchant;
import com.campus.delivery.service.CategoryService;
import com.campus.delivery.service.MerchantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/merchant/category")
public class MerchantCategoryController {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private MerchantService merchantService;

    @GetMapping("/list")
    public Result<List<Category>> list(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Merchant merchant = merchantService.getByUserId(userId);
        if (merchant == null) {
            return Result.error("商户不存在");
        }
        List<Category> list = categoryService.listByMerchantId(merchant.getId());
        return Result.success(list);
    }

    @PostMapping("/add")
    public Result<Void> add(@RequestBody Category category, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Merchant merchant = merchantService.getByUserId(userId);
        if (merchant == null) {
            return Result.error("商户不存在");
        }
        try {
            categoryService.createCategory(merchant.getId(), category);
            return Result.success("添加成功", null);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Category category, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Merchant merchant = merchantService.getByUserId(userId);
        if (merchant == null) {
            return Result.error("商户不存在");
        }
        try {
            categoryService.updateCategory(merchant.getId(), id, category);
            return Result.success("更新成功", null);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Merchant merchant = merchantService.getByUserId(userId);
        if (merchant == null) {
            return Result.error("商户不存在");
        }
        try {
            categoryService.deleteCategory(merchant.getId(), id);
            return Result.success("删除成功", null);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }
}