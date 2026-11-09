package com.SwiftBasket.notification_service.consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.SwiftBasket.notification_service.event.PaymentCompletedEvent;
import com.SwiftBasket.notification_service.service.NotificationService;

import lombok.extern.slf4j.Slf4j;

/**
 * Consumes payment.completed (keyed by orderId) and sends the payment
 * receipt email. Email is resolved at send time via the claim-check pattern.
 */
@Component
@Slf4j
public class PaymentCompletedConsumer {

    @Autowired
    private NotificationService notificationService;

    @KafkaListener(topics = "${kafka_topic.payment_completed}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(PaymentCompletedEvent event) {
        log.info("Received payment.completed event - paymentId: {}, orderId: {}",
                event.getPaymentId(), event.getOrderId());
        try {
            notificationService.sendPaymentReceipt(event);
        } catch (Exception e) {
            log.error("Unexpected error processing payment.completed paymentId {}: {}",
                    event.getPaymentId(), e.getMessage());
        }
    }
}
