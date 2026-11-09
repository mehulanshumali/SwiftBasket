package com.SwiftBasket.cart_service.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.SwiftBasket.cart_service.document.Cart;
import com.SwiftBasket.cart_service.document.CartItem;
import com.SwiftBasket.cart_service.dto.CartItemResponse;
import com.SwiftBasket.cart_service.dto.CartResponse;

@Component
public class CartMapper {

    public CartResponse toResponse(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream()
                .map(this::toItemResponse)
                .toList();

        return CartResponse.builder()
                .cartId(cart.getId())
                .items(items)
                .totalPrice(cart.getTotalPrice())
                .build();
    }

    private CartItemResponse toItemResponse(CartItem item) {
        return CartItemResponse.builder()
                .productId(item.getProductId())
                .name(item.getName())
                .imageUrl(item.getImageUrl())
                .unitPrice(item.getUnitPrice())
                .quantity(item.getQuantity())
                .lineTotal(item.getUnitPrice()
                        .multiply(java.math.BigDecimal.valueOf(item.getQuantity())))
                .build();
    }
}
