package com.SwiftBasket.cart_service.dto;

import java.math.BigDecimal;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "CartResponse", description = "Shopping cart with items and totals")
public class CartResponse {

    @Schema(description = "Cart's unique ID")
    private String cartId;

    @Schema(description = "Items currently in the cart")
    private List<CartItemResponse> items;

    @Schema(description = "Total price of all items in the cart")
    private BigDecimal totalPrice;

    @Schema(description = "ISO currency code for cart prices")
    private String currency;
}
