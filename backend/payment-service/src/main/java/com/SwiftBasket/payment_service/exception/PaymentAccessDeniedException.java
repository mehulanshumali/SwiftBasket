package com.SwiftBasket.payment_service.exception;

public class PaymentAccessDeniedException extends RuntimeException {
    public PaymentAccessDeniedException(String message) {
        super(message);
    }
}
