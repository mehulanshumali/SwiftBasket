package com.SwiftBasket.product_service.mapper;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.SwiftBasket.product_service.document.ProductDocument;
import com.SwiftBasket.product_service.dto.request.ProductRequest;
import com.SwiftBasket.product_service.dto.response.ProductResponse;
import com.SwiftBasket.product_service.dto.response.ProductSearchResponse;
import com.SwiftBasket.product_service.entity.Category;
import com.SwiftBasket.product_service.entity.Product;

@Component
public class ProductMapper {

    public ProductResponse toResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .categoryName(product.getCategory().getName())
                .categoryId(product.getCategory().getId())
                .imageUrl(product.getImageUrl())
                .sku(product.getSku())
                .active(product.getActive())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    public ProductDocument toDocument(Product product) {
        return ProductDocument.builder()
                .id(String.valueOf(product.getId()))
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

    public Product toEntity(ProductRequest request, Category category) {
        return Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stockQuantity(request.getStockQuantity() == null ? 0 : request.getStockQuantity())
                .category(category)
                .sku(request.getSku())
                .active(true)
                .imageUrl(request.getImageUrl())
                .build();
    }
}
