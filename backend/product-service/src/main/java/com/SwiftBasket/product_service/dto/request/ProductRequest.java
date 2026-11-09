package com.SwiftBasket.product_service.dto.request;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(name = "ProductRequest", description = "Create or update product request")
public class ProductRequest {

    @Schema(description = "Product name")
    @NotBlank(message = "Product name is required")
    @Size(min = 3, max = 255, message = "Product name must be between 3 and 255 characters")
    private String name;

    @Schema(description = "Product description")
    @NotBlank(message = "Product description is required")
    private String description;

    @Schema(description = "Product price")
    @NotNull(message = "Product price is required")
    @Positive(message = "Product price must be greater than zero")
    private BigDecimal price;

    @Schema(description = "Initial stock quantity")
    @Min(value = 0, message = "Stock quantity cannot be negative")
    private Integer stockQuantity;

    @Schema(description = "Product image URL")
    private String imageUrl;

    @Schema(description = "Stock keeping unit - unique product code")
    @NotBlank(message = "SKU is required")
    private String sku;

    @Schema(description = "Category the product belongs to")
    @NotNull(message = "Category ID is required")
    private Long categoryId;
}
