package com.SwiftBasket.notification_service.service;

public interface EmailSender {
    void send(String to, String subject, String htmlBody);
}
