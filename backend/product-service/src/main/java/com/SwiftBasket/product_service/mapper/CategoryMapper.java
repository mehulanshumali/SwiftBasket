package com.SwiftBasket.product_service.mapper;

import org.springframework.stereotype.Component;

import com.SwiftBasket.product_service.dto.response.CategoryResponse;
import com.SwiftBasket.product_service.entity.Category;

@Component
public class CategoryMapper {

    public CategoryResponse toResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .build();
    }
}
