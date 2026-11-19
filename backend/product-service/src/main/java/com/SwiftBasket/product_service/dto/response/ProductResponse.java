package com.SwiftBasket.product_service.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ProductResponse", description = "Product details")
public class ProductResponse {

    @Schema(description = "Product's unique ID")
    private Long id;

    @Schema(description = "Product name")
    private String name;

    @Schema(description = "Product description")
    private String description;

    @Schema(description = "Product price")
    private BigDecimal price;

    @Schema(description = "ISO currency code for the product price")
    private String currency;

    @Schema(description = "Quantity in stock")
    private Integer stockQuantity;

    @Schema(description = "Category name")
    private String categoryName;

    @Schema(description = "Category's unique ID")
    private Long categoryId;

    @Schema(description = "Product image URL")
    private String imageUrl;

    @Schema(description = "Stock keeping unit")
    private String sku;

    @Schema(description = "Whether the product is active in the catalog")
    private Boolean active;

    @Schema(description = "Product creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedAt;
}
