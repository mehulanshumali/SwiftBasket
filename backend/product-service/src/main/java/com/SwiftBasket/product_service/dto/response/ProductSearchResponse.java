package com.SwiftBasket.product_service.dto.response;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ProductSearchResponse", description = "Product search result from Elasticsearch")
public class ProductSearchResponse {

    @Schema(description = "Product's unique ID")
    private String id;

    @Schema(description = "Product name")
    private String name;

    @Schema(description = "Product description")
    private String description;

    @Schema(description = "Product price")
    private BigDecimal price;

    @Schema(description = "ISO currency code for the product price")
    private String currency;

    @Schema(description = "Category name")
    private String categoryName;

    @Schema(description = "Product image URL")
    private String imageUrl;

    @Schema(description = "Whether the product is active in the catalog")
    private boolean active;
}
