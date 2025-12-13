package com.SwiftBasket.product_service.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProductRequest {
    @NotBlank(message="Product name is required")
    @Size(min=3, max=255, message="Product name must be between 3 and 255 characters")
    private String name;

    @NotBlank(message="Product description is required")
    private String description;

    @NotNull(message="Product price is required")
    @Positive(message="Product price must be greater than zero")
    private BigDecimal price;

    @Min(value = 0, message = "Stock quantity cannot be negative")
    private Integer stockQuantity;

    private String imageUrl;

    @NotBlank(message="SKU is required")
    private String sku;

    @NotNull(message="Category ID is required")
    private Long categoryId;

}
