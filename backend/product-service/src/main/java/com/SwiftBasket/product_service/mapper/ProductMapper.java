package com.SwiftBasket.product_service.mapper;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.SwiftBasket.product_service.document.ProductDocument;
import com.SwiftBasket.product_service.dto.request.CategoryRequest;
import com.SwiftBasket.product_service.dto.request.ProductRequest;
import com.SwiftBasket.product_service.dto.response.CategoryResponse;
import com.SwiftBasket.product_service.dto.response.PagedResponse;
import com.SwiftBasket.product_service.dto.response.ProductResponse;
import com.SwiftBasket.product_service.dto.response.ProductSearchResponse;
import com.SwiftBasket.product_service.entity.Category;
import com.SwiftBasket.product_service.entity.Product;

@Component
public class ProductMapper {
    public Product toProductEntity(ProductRequest request, Category category) {
        return Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .category(category)
                .imageUrl(request.getImageUrl())
                .sku(request.getSku())
                .active(true)
                .build();
    }

    public ProductResponse toProductResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .categoryId(product.getCategory().getId())
                .categoryName(product.getCategory().getName())
                .imageUrl(product.getImageUrl())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    public ProductDocument toProductDocument(Product product) {
        return ProductDocument.builder()
                .id(product.getId().toString()) // Convert Long to String
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .categoryName(product.getCategory().getName())
                .imageUrl(product.getImageUrl())
                .build();
    }

    public ProductSearchResponse toSearchResponse(ProductDocument document) {
        return ProductSearchResponse.builder()
                .id(document.getId())
                .name(document.getName())
                .description(document.getDescription())
                .price(document.getPrice())
                .categoryName(document.getCategoryName())
                .imageUrl(document.getImageUrl())
                .build();
    }

    public Category toCategoryEntity(CategoryRequest request) {
        return Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
    }

    public CategoryResponse toCategoryResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .build();
    }

    public <T> PagedResponse<T> toPagedResponse(Page<?> page, List<T> mappedContent) {
        return PagedResponse.<T>builder()
                .content(mappedContent)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
    
}