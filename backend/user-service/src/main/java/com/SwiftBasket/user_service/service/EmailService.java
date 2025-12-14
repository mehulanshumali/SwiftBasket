package com.SwiftBasket.user_service.service;

public interface EmailService {
    void sendPasswordResetEmail(String email, String fullName, String resetLink);
    void sendRegistrationConfirmationEmail(String email, String fullName);
}