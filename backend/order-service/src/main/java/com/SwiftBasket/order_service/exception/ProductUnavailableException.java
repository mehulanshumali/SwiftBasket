package com.SwiftBasket.order_service.exception;

public class ProductUnavailableException extends RuntimeException {
    public ProductUnavailableException(String message) {
        super(message);
    }
}
