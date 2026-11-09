package com.SwiftBasket.order_service.dto;

import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "TrackingResponse", description = "Delivery status timeline for an order")
public class TrackingResponse {

    @Schema(description = "Order's unique ID")
    private Long orderId;

    @Schema(description = "Current order status")
    private String currentStatus;

    @Schema(description = "Ordered status timeline entries, oldest first")
    private List<StatusEntry> timeline;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "StatusEntry", description = "A single status change in the order timeline")
    public static class StatusEntry {

        @Schema(description = "Previous status, null for the initial entry")
        private String fromStatus;

        @Schema(description = "New status")
        private String toStatus;

        @Schema(description = "Optional note, e.g. courier AWB number")
        private String note;

        @Schema(description = "When the change happened")
        private LocalDateTime changedAt;
    }
}
