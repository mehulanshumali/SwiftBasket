package com.SwiftBasket.order_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.SwiftBasket.order_service.entity.OrderStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "OrderResponse", description = "Order with line items and totals")
public class OrderResponse {

    @Schema(description = "Order's unique ID")
    private Long orderId;

    @Schema(description = "ID of the user who placed the order")
    private Long userId;

    @Schema(description = "Current order status")
    private String status;

    @Schema(description = "Next statuses an admin may apply to this order")
    private List<OrderStatus> allowedAdminStatuses;

    @Schema(description = "Total amount for the order")
    private BigDecimal totalAmount;

    @Schema(description = "Currency code")
    private String currency;

    @Schema(description = "Shipping address captured at order time")
    private String shippingAddress;

    @Schema(description = "Line items in the order")
    private List<OrderItemResponse> items;

    @Schema(description = "Order creation timestamp")
    private LocalDateTime createdAt;
}
