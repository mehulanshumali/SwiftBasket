package com.SwiftBasket.notification_service.service;

import com.SwiftBasket.notification_service.event.OrderPlacedEvent;
import com.SwiftBasket.notification_service.event.OrderStatusUpdatedEvent;
import com.SwiftBasket.notification_service.event.PaymentCompletedEvent;
import com.SwiftBasket.notification_service.event.UserRegisteredEvent;

public interface NotificationService {
    void sendWelcomeEmail(UserRegisteredEvent event);
    void sendOrderConfirmation(OrderPlacedEvent event);
    void sendPaymentReceipt(PaymentCompletedEvent event);
    void sendStatusUpdate(OrderStatusUpdatedEvent event);
}
