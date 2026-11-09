package com.SwiftBasket.cart_service.service;

import com.SwiftBasket.cart_service.document.Cart;
import com.SwiftBasket.cart_service.dto.AddItemRequest;

public interface CartService {
    Cart getCart(Long userId, String sessionId);
    Cart addItem(Long userId, String sessionId, AddItemRequest request);
    Cart updateQuantity(Long userId, String sessionId, Long productId, int quantity);
    Cart removeItem(Long userId, String sessionId, Long productId);
    void clearCart(Long userId, String sessionId);
    Cart mergeGuestCart(Long userId, String sessionId);
}
