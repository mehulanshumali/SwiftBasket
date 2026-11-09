package com.SwiftBasket.notification_service.mapper;

import org.springframework.stereotype.Component;

import com.SwiftBasket.notification_service.dto.NotificationResponse;
import com.SwiftBasket.notification_service.entity.Notification;

@Component
public class NotificationMapper {

    public NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .recipientEmail(notification.getRecipientEmail())
                .type(notification.getType().name())
                .subject(notification.getSubject())
                .status(notification.getStatus().name())
                .kafkaTopic(notification.getKafkaTopic())
                .createdAt(notification.getCreatedAt())
                .sentAt(notification.getSentAt())
                .build();
    }
}
