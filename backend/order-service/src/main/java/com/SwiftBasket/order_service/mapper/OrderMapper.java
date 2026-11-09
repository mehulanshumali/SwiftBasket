package com.SwiftBasket.order_service.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.SwiftBasket.order_service.dto.OrderItemResponse;
import com.SwiftBasket.order_service.dto.OrderResponse;
import com.SwiftBasket.order_service.dto.TrackingResponse;
import com.SwiftBasket.order_service.entity.Order;
import com.SwiftBasket.order_service.entity.OrderItem;
import com.SwiftBasket.order_service.entity.OrderStatusHistory;

@Component
public class OrderMapper {

    public OrderResponse toResponse(Order order, List<OrderItem> items) {
        return OrderResponse.builder()
                .orderId(order.getId())
                .status(order.getStatus().name())
                .totalAmount(order.getTotalAmount())
                .currency(order.getCurrency())
                .shippingAddress(order.getShippingAddress())
                .items(items.stream().map(this::toItemResponse).toList())
                .createdAt(order.getCreatedAt())
                .build();
    }

    public OrderItemResponse toItemResponse(OrderItem item) {
        return OrderItemResponse.builder()
                .productId(item.getProductId())
                .productName(item.getProductName())
                .unitPrice(item.getUnitPrice())
                .quantity(item.getQuantity())
                .build();
    }

    public TrackingResponse toTrackingResponse(Order order, List<OrderStatusHistory> history) {
        return TrackingResponse.builder()
                .orderId(order.getId())
                .currentStatus(order.getStatus().name())
                .timeline(history.stream()
                        .map(entry -> TrackingResponse.StatusEntry.builder()
                                .fromStatus(entry.getFromStatus())
                                .toStatus(entry.getToStatus())
                                .note(entry.getNote())
                                .changedAt(entry.getChangedAt())
                                .build())
                        .toList())
                .build();
    }
}
