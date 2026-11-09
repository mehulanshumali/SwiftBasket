package com.SwiftBasket.notification_service.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "NotificationResponse", description = "An audit record of a sent (or failed) notification email")
public class NotificationResponse {

    @Schema(description = "Notification's ID")
    private Long id;

    @Schema(description = "Recipient email address at send time")
    private String recipientEmail;

    @Schema(description = "Notification type: WELCOME, ORDER_CONFIRMATION, PAYMENT_RECEIPT, ORDER_STATUS_UPDATE")
    private String type;

    @Schema(description = "Email subject line")
    private String subject;

    @Schema(description = "Delivery status: PENDING, SENT, FAILED")
    private String status;

    @Schema(description = "Kafka topic that triggered this notification")
    private String kafkaTopic;

    @Schema(description = "When the notification record was created")
    private LocalDateTime createdAt;

    @Schema(description = "When the email was successfully sent (null if not sent)")
    private LocalDateTime sentAt;
}
