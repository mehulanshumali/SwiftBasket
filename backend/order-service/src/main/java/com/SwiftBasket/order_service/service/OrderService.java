package com.SwiftBasket.order_service.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.SwiftBasket.order_service.dto.CheckoutRequest;
import com.SwiftBasket.order_service.entity.Order;

public interface OrderService {
    Order checkout(Long userId, CheckoutRequest request);
    Order markPaid(Long orderId, String stripePaymentIntentId);
    Order markPaymentFailed(Long orderId, String failureReason);
    Page<Order> getUserOrders(Long userId, Pageable pageable);
    Order getOrderForUser(Long orderId, Long userId);
    Order updateStatus(Long orderId, String newStatus, String note);
}
