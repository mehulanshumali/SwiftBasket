package com.SwiftBasket.product_service.service;

import java.util.List;

import com.SwiftBasket.product_service.dto.request.CategoryRequest;
import com.SwiftBasket.product_service.dto.response.CategoryResponse;
import com.SwiftBasket.product_service.entity.Category;

public interface CategoryService {
    List<Category> getAllCategories();
    Category createCategory(CategoryRequest request);
    Category updateCategory(Long categoryId, CategoryRequest request);
    void deleteCategory(Long categoryId);
}
