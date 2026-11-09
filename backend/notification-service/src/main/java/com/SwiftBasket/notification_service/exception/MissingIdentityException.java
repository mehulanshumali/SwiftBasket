package com.SwiftBasket.notification_service.exception;

public class MissingIdentityException extends RuntimeException {
    public MissingIdentityException(String message) {
        super(message);
    }
}
