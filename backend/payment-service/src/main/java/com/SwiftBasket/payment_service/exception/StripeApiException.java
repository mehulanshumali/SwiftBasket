package com.SwiftBasket.payment_service.exception;

public class StripeApiException extends RuntimeException {
    public StripeApiException(String message) {
        super(message);
    }
}
