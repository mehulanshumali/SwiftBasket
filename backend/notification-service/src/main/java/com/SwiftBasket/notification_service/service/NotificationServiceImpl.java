package com.SwiftBasket.notification_service.service;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.SwiftBasket.notification_service.client.UserClient;
import com.SwiftBasket.notification_service.entity.Notification;
import com.SwiftBasket.notification_service.entity.NotificationStatus;
import com.SwiftBasket.notification_service.entity.NotificationType;
import com.SwiftBasket.notification_service.event.OrderPlacedEvent;
import com.SwiftBasket.notification_service.event.OrderStatusUpdatedEvent;
import com.SwiftBasket.notification_service.event.PaymentCompletedEvent;
import com.SwiftBasket.notification_service.event.UserRegisteredEvent;
import com.SwiftBasket.notification_service.repository.NotificationRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Saves each notification PENDING, sends, then flips to SENT/FAILED - the
 * audit row survives both success and failure. User-service being down marks
 * the notification FAILED but never crashes the consumer (claim-check
 * resolution failure is a data problem, not a poison pill).
 */
@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private EmailSender emailSender;

    @Autowired
    private UserClient userClient;

    @Autowired
    private EmailTemplates templates;

    @Override
    public void sendWelcomeEmail(UserRegisteredEvent event) {
        send(event.getUserId(),
                event.getEmail(), // welcome event carries email directly (source of truth, emitted once)
                "Welcome to SwiftBasket",
                templates.welcome(event.getFullName()),
                NotificationType.WELCOME,
                "user.registered");
    }

    @Override
    public void sendOrderConfirmation(OrderPlacedEvent event) {
        send(event.getUserId(),
                null, // resolve at send time via claim-check
                "Order #" + event.getOrderId() + " confirmed",
                templates.orderConfirmation(event.getOrderId(), event.getItems(),
                        event.getTotalAmount(), event.getCurrency()),
                NotificationType.ORDER_CONFIRMATION,
                "order.placed");
    }

    @Override
    public void sendPaymentReceipt(PaymentCompletedEvent event) {
        send(event.getUserId(),
                null,
                "Payment receipt for order #" + event.getOrderId(),
                templates.paymentReceipt(event.getPaymentId(), event.getOrderId(),
                        event.getAmount(), event.getCurrency()),
                NotificationType.PAYMENT_RECEIPT,
                "payment.completed");
    }

    @Override
    public void sendStatusUpdate(OrderStatusUpdatedEvent event) {
        send(event.getUserId(),
                null,
                "Order #" + event.getOrderId() + " is now " + event.getToStatus(),
                templates.statusUpdate(event.getOrderId(), event.getToStatus(), event.getNote()),
                NotificationType.ORDER_STATUS_UPDATE,
                "order.status.updated");
    }

    private void send(Long userId, String directEmail, String subject, String htmlBody,
                      NotificationType type, String kafkaTopic) {

        // 1. Resolve recipient email (claim-check) - failure => FAILED audit row, no crash
        String email = directEmail;
        if (email == null) {
            try {
                email = userClient.getEmail(userId);
            } catch (Exception e) {
                log.error("Could not resolve email for userId {} - recording FAILED notification", userId);
                saveFailed(userId, subject, htmlBody, type, kafkaTopic, "Email resolution failed");
                return;
            }
        }

        // 2. Persist PENDING
        Notification notification = notificationRepository.save(Notification.builder()
                .recipientEmail(email)
                .type(type)
                .subject(subject)
                .content(htmlBody)
                .status(NotificationStatus.PENDING)
                .kafkaTopic(kafkaTopic)
                .build());
        log.info("Notification PENDING - id: {}, type: {}, topic: {}", notification.getId(), type, kafkaTopic);

        // 3. Send + flip to SENT / FAILED
        try {
            emailSender.send(email, subject, htmlBody);
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            notificationRepository.save(notification);
            log.info("Notification SENT - id: {}, recipient: {}", notification.getId(), email);
        } catch (Exception e) {
            notification.setStatus(NotificationStatus.FAILED);
            notificationRepository.save(notification);
            log.error("Notification FAILED - id: {}, recipient: {}: {}", notification.getId(), email, e.getMessage());
        }
    }

    private void saveFailed(Long userId, String subject, String content,
                            NotificationType type, String kafkaTopic, String reason) {
        notificationRepository.save(Notification.builder()
                .recipientEmail("unresolved:user:" + userId)
                .type(type)
                .subject(subject)
                .content(content)
                .status(NotificationStatus.FAILED)
                .kafkaTopic(kafkaTopic)
                .build());
    }
}
