package com.campus.delivery.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.delivery.entity.Category;
import com.campus.delivery.entity.Dish;
import com.campus.delivery.mapper.CategoryMapper;
import com.campus.delivery.service.CategoryService;
import com.campus.delivery.service.DishService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {
    @Autowired
    private DishService dishService;

    @Override
    public List<Category> listByMerchantId(Long merchantId) {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Category::getMerchantId, merchantId)
                .orderByAsc(Category::getSort)
                .orderByAsc(Category::getId);
        return this.list(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Category resolveCategory(Long merchantId, Long categoryId, String categoryName) {
        if (categoryId != null) {
            Category category = this.getById(categoryId);
            if (category == null || !merchantId.equals(category.getMerchantId())) {
                throw new IllegalArgumentException("\u5206\u7c7b\u4e0d\u5b58\u5728\u6216\u65e0\u6743\u9650\u64cd\u4f5c");
            }
            return category;
        }

        String normalizedName = normalizeName(categoryName);

        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Category::getMerchantId, merchantId)
                .eq(Category::getName, normalizedName)
                .last("LIMIT 1");
        Category existing = this.getOne(wrapper, false);
        if (existing != null) {
            return existing;
        }

        Category category = new Category();
        category.setMerchantId(merchantId);
        category.setName(normalizedName);
        category.setSort(nextSort(merchantId));
        this.save(category);
        return category;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createCategory(Long merchantId, Category payload) {
        String normalizedName = normalizeName(payload.getName());
        validateNameNotDuplicate(merchantId, normalizedName, null);

        Category category = new Category();
        category.setMerchantId(merchantId);
        category.setName(normalizedName);
        category.setSort(normalizeSort(payload.getSort()));
        this.save(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCategory(Long merchantId, Long categoryId, Category payload) {
        Category current = this.getById(categoryId);
        if (current == null || !merchantId.equals(current.getMerchantId())) {
            throw new IllegalArgumentException("\u5206\u7c7b\u4e0d\u5b58\u5728\u6216\u65e0\u6743\u9650\u64cd\u4f5c");
        }

        String normalizedName = normalizeName(payload.getName());
        validateNameNotDuplicate(merchantId, normalizedName, categoryId);

        Category category = new Category();
        category.setId(categoryId);
        category.setName(normalizedName);
        category.setSort(normalizeSort(payload.getSort()));
        this.updateById(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(Long merchantId, Long categoryId) {
        Category current = this.getById(categoryId);
        if (current == null || !merchantId.equals(current.getMerchantId())) {
            throw new IllegalArgumentException("\u5206\u7c7b\u4e0d\u5b58\u5728\u6216\u65e0\u6743\u9650\u64cd\u4f5c");
        }

        LambdaQueryWrapper<Dish> dishWrapper = new LambdaQueryWrapper<>();
        dishWrapper.eq(Dish::getMerchantId, merchantId)
                .eq(Dish::getCategoryId, categoryId);
        long count = dishService.count(dishWrapper);
        if (count > 0) {
            throw new IllegalArgumentException("\u5206\u7c7b\u4e0b\u4ecd\u6709\u83dc\u54c1\uff0c\u65e0\u6cd5\u5220\u9664");
        }
        this.removeById(categoryId);
    }

    private String normalizeName(String name) {
        String normalized = name == null ? null : name.trim();
        if (!StringUtils.hasText(normalized)) {
            throw new IllegalArgumentException("\u5206\u7c7b\u540d\u79f0\u4e0d\u80fd\u4e3a\u7a7a");
        }
        return normalized;
    }

    private int normalizeSort(Integer sort) {
        int value = sort == null ? 0 : sort;
        if (value < 0) {
            throw new IllegalArgumentException("\u6392\u5e8f\u503c\u4e0d\u80fd\u5c0f\u4e8e 0");
        }
        return value;
    }

    private void validateNameNotDuplicate(Long merchantId, String name, Long excludeId) {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Category::getMerchantId, merchantId)
                .eq(Category::getName, name);
        if (excludeId != null) {
            wrapper.ne(Category::getId, excludeId);
        }
        if (this.count(wrapper) > 0) {
            throw new IllegalArgumentException("\u5206\u7c7b\u540d\u79f0\u5df2\u5b58\u5728");
        }
    }

    private int nextSort(Long merchantId) {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Category::getMerchantId, merchantId)
                .orderByDesc(Category::getSort)
                .orderByDesc(Category::getId)
                .last("LIMIT 1");
        Category lastCategory = this.getOne(wrapper, false);
        if (lastCategory == null || lastCategory.getSort() == null) {
            return 0;
        }
        return lastCategory.getSort() + 1;
    }
}
