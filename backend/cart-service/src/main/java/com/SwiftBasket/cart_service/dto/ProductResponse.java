package com.SwiftBasket.cart_service.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(name = "ProductResponse", description = "Product details snapshotted from the product service")
public class ProductResponse {

    @Schema(description = "Product's unique ID")
    private Long id;

    @Schema(description = "Product name")
    private String name;

    @Schema(description = "Product image URL")
    private String imageUrl;

    @Schema(description = "Product unit price")
    private BigDecimal price;
}
