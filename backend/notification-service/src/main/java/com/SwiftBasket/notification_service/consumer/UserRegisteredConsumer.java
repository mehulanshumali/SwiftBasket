package com.SwiftBasket.notification_service.consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.SwiftBasket.notification_service.event.UserRegisteredEvent;
import com.SwiftBasket.notification_service.service.NotificationService;

import lombok.extern.slf4j.Slf4j;

/**
 * Consumes user.registered (keyed by userId) and sends the welcome email.
 * This is the only event whose payload carries the email directly - it is
 * emitted exactly once by the source of truth at registration time.
 */
@Component
@Slf4j
public class UserRegisteredConsumer {

    @Autowired
    private NotificationService notificationService;

    @KafkaListener(topics = "${kafka_topic.user_registered}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(UserRegisteredEvent event) {
        log.info("Received user.registered event - userId: {}", event.getUserId());
        try {
            notificationService.sendWelcomeEmail(event);
        } catch (Exception e) {
            log.error("Unexpected error processing user.registered userId {}: {}",
                    event.getUserId(), e.getMessage());
        }
    }
}
