package com.SwiftBasket.notification_service.event;

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
@Schema(name = "OrderStatusUpdatedEvent", description = "Kafka event from order-service; triggers the delivery status update email")
public class OrderStatusUpdatedEvent {

    @Schema(description = "Order's ID")
    private Long orderId;

    @Schema(description = "Customer (claim-check: email resolved at send time)")
    private Long userId;

    @Schema(description = "Previous status")
    private String fromStatus;

    @Schema(description = "New status")
    private String toStatus;

    @Schema(description = "Human-readable context, e.g. courier AWB number")
    private String note;

    @Schema(description = "Transition timestamp")
    private LocalDateTime updatedAt;
}
