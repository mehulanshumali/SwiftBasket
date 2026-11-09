package com.SwiftBasket.order_service.event;

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
@Schema(name = "OrderStatusUpdatedEvent", description = "Kafka event published on every order status change (payment events and admin actions); triggers the delivery status update email")
public class OrderStatusUpdatedEvent {

    @Schema(description = "Order's ID - also the Kafka message key")
    private Long orderId;

    @Schema(description = "Customer (claim-check: no email)")
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
