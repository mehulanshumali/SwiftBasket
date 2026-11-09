package com.SwiftBasket.product_service.exception;

public class AdminAccessException extends RuntimeException {
    public AdminAccessException(String message) {
        super(message);
    }
}
