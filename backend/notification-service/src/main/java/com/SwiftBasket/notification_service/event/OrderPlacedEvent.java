package com.SwiftBasket.notification_service.event;

import java.math.BigDecimal;
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
@Schema(name = "OrderPlacedEvent", description = "Kafka event from order-service; triggers the order confirmation email")
public class OrderPlacedEvent {

    @Schema(description = "New order's ID")
    private Long orderId;

    @Schema(description = "Buyer (claim-check: email resolved at send time)")
    private Long userId;

    @Schema(description = "Order total (sum of unitPrice x quantity)")
    private BigDecimal totalAmount;

    @Schema(description = "3-letter ISO currency code (INR)")
    private String currency;

    @Schema(description = "Line-item snapshots taken at checkout")
    private List<EventItem> items;

    @Schema(description = "Checkout timestamp")
    private LocalDateTime placedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "OrderPlacedEventItem", description = "A line-item snapshot in the order.placed event")
    public static class EventItem {        @Schema(description = "Product ID")
        private Long productId;

        @Schema(description = "Product name captured at purchase time")
        private String productName;

        @Schema(description = "Unit price captured at purchase time")
        private BigDecimal unitPrice;

        @Schema(description = "Units ordered (reserved from stock)")
        private Integer quantity;
    }
}
