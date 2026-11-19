package com.SwiftBasket.order_service.entity;

import java.util.List;

public enum OrderStatus {
    PENDING,
    PAID,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    PAYMENT_FAILED,
    CANCELLED;

    public List<OrderStatus> allowedAdminStatuses() {
        return switch (this) {
            case PENDING -> List.of(PAID, CANCELLED);
            case PAID -> List.of(PROCESSING, CANCELLED);
            case PROCESSING -> List.of(SHIPPED);
            case SHIPPED -> List.of(DELIVERED);
            case DELIVERED, PAYMENT_FAILED, CANCELLED -> List.of();
        };
    }
}
