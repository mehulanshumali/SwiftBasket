package com.SwiftBasket.notification_service.consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.SwiftBasket.notification_service.event.OrderStatusUpdatedEvent;
import com.SwiftBasket.notification_service.service.NotificationService;

import lombok.extern.slf4j.Slf4j;

/**
 * Consumes order.status.updated (keyed by orderId) and sends the delivery
 * status update email. Email is resolved at send time via the claim-check
 * pattern (userId -> current email from user-service).
 */
@Component
@Slf4j
public class OrderStatusUpdatedConsumer {

    @Autowired
    private NotificationService notificationService;

    @KafkaListener(topics = "${kafka_topic.order_status_updated}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(OrderStatusUpdatedEvent event) {
        log.info("Received order.status.updated event - orderId: {}, {} -> {}",
                event.getOrderId(), event.getFromStatus(), event.getToStatus());
        try {
            notificationService.sendStatusUpdate(event);
        } catch (Exception e) {
            log.error("Unexpected error processing order.status.updated orderId {}: {}",
                    event.getOrderId(), e.getMessage());
        }
    }
}
