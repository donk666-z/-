package com.campus.delivery.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.delivery.entity.Category;
import java.util.List;

public interface CategoryService extends IService<Category> {
    List<Category> listByMerchantId(Long merchantId);

    Category resolveCategory(Long merchantId, Long categoryId, String categoryName);

    void createCategory(Long merchantId, Category payload);

    void updateCategory(Long merchantId, Long categoryId, Category payload);

    void deleteCategory(Long merchantId, Long categoryId);
}
