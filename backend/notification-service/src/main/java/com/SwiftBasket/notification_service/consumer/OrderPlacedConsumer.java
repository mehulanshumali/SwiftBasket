package com.SwiftBasket.notification_service.consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.SwiftBasket.notification_service.event.OrderPlacedEvent;
import com.SwiftBasket.notification_service.service.NotificationService;

import lombok.extern.slf4j.Slf4j;

/**
 * Consumes order.placed (keyed by orderId) and sends the order confirmation
 * email. At-least-once delivery means duplicates are possible; welcome/
 * confirmation emails are idempotent from the customer's perspective.
 */
@Component
@Slf4j
public class OrderPlacedConsumer {

    @Autowired
    private NotificationService notificationService;

    @KafkaListener(topics = "${kafka_topic.order_placed}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(OrderPlacedEvent event) {
        log.info("Received order.placed event - orderId: {}, userId: {}", event.getOrderId(), event.getUserId());
        try {
            notificationService.sendOrderConfirmation(event);
        } catch (Exception e) {
            // should never reach here (service handles its own failures), last-resort guard
            log.error("Unexpected error processing order.placed orderId {}: {}", event.getOrderId(), e.getMessage());
        }
    }
}
