package com.SwiftBasket.cart_service.dto;

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
@Schema(name = "CartItemResponse", description = "A single product line in the cart")
public class CartItemResponse {

    @Schema(description = "Product's unique ID")
    private Long productId;

    @Schema(description = "Product name at the time it was added")
    private String name;

    @Schema(description = "Product image URL")
    private String imageUrl;

    @Schema(description = "Unit price at the time the product was added")
    private BigDecimal unitPrice;

    @Schema(description = "Quantity in the cart")
    private int quantity;

    @Schema(description = "Total price for this line (unitPrice x quantity)")
    private BigDecimal lineTotal;
}
