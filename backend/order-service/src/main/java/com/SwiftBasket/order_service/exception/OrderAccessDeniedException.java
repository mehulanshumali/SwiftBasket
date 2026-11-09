package com.SwiftBasket.order_service.exception;

public class OrderAccessDeniedException extends RuntimeException {
    public OrderAccessDeniedException(String message) {
        super(message);
    }
}
