package com.SwiftBasket.order_service.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.SwiftBasket.order_service.client.CartClient;
import com.SwiftBasket.order_service.client.CartClient.CartData;
import com.SwiftBasket.order_service.client.CartClient.CartItemData;
import com.SwiftBasket.order_service.client.ProductClient;
import com.SwiftBasket.order_service.dto.CheckoutRequest;
import com.SwiftBasket.order_service.entity.Order;
import com.SwiftBasket.order_service.entity.OrderItem;
import com.SwiftBasket.order_service.entity.OrderStatus;
import com.SwiftBasket.order_service.entity.OrderStatusHistory;
import com.SwiftBasket.order_service.event.OrderPlacedEvent;
import com.SwiftBasket.order_service.event.OrderStatusUpdatedEvent;
import com.SwiftBasket.order_service.exception.CartEmptyException;
import com.SwiftBasket.order_service.exception.InvalidStatusTransitionException;
import com.SwiftBasket.order_service.exception.OrderAccessDeniedException;
import com.SwiftBasket.order_service.exception.OrderNotFoundException;
import com.SwiftBasket.order_service.repository.OrderItemRepository;
import com.SwiftBasket.order_service.repository.OrderRepository;
import com.SwiftBasket.order_service.repository.OrderStatusHistoryRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Value("${swiftbasket.currency:INR}")
    private String currency;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Autowired
    private CartClient cartClient;

    @Autowired
    private ProductClient productClient;

    @Autowired
    private KafkaTemplate<String, OrderPlacedEvent> orderPlacedKafkaTemplate;

    @Autowired
    private KafkaTemplate<String, OrderStatusUpdatedEvent> orderStatusUpdatedKafkaTemplate;

    @Value("${kafka_topic.order_placed}")
    private String orderPlacedTopic;

    @Value("${kafka_topic.order_status_updated}")
    private String orderStatusUpdatedTopic;

    @Override
    public Order checkout(Long userId, CheckoutRequest request) {
        log.info("Checkout request for user {} - shipping to: {}", userId, request.getShippingAddress());

        // stage 1: fetch the cart - nothing to order without items
        CartData cart = cartClient.getCart(userId);
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            log.warn("Checkout blocked for user {} - cart is empty", userId);
            throw new CartEmptyException("Cart is empty");
        }

        // stage 2: reserve stock item by item; roll back on any failure
        List<CartItemData> reservedItems = new ArrayList<>();
        try {
            for (CartItemData item : cart.getItems()) {
                productClient.reserveStock(item.getProductId(), item.getQuantity());
                reservedItems.add(item);
            }
        } catch (RuntimeException e) {
            log.warn("Stock reservation failed for user {} - rolling back {} reserved items",
                    userId, reservedItems.size());
            rollbackReservations(reservedItems);
            throw e;
        }

        // stage 3: persist order, item snapshots and initial history row
        Order order;
        try {
            order = saveOrder(userId, request, cart.getItems());
        } catch (RuntimeException e) {
            // DB save failed after successful reservations - release everything
            log.error("Order save failed for user {} - releasing {} reservations: {}",
                    userId, reservedItems.size(), e.getMessage());
            rollbackReservations(reservedItems);
            throw e;
        }

        // stage 4: publish order.placed - best-effort, never fails checkout
        publishOrderPlaced(order);

        // stage 5: clear the cart - cosmetic failure, never fails checkout
        cartClient.clearCart(userId);

        log.info("Checkout complete - orderId: {}, status: {}, total: {}",
                order.getId(), order.getStatus(), order.getTotalAmount());
        return order;
    }

    @Transactional
    protected Order saveOrder(Long userId, CheckoutRequest request, List<CartItemData> items) {
        BigDecimal totalAmount = items.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = orderRepository.save(Order.builder()
                .userId(userId)
                .status(OrderStatus.PENDING)
                .totalAmount(totalAmount)
                .currency(currency)
                .shippingAddress(request.getShippingAddress())
                .build());

        for (CartItemData item : items) {
            orderItemRepository.save(OrderItem.builder()
                    .order(order)
                    .productId(item.getProductId())
                    .productName(item.getName())
                    .unitPrice(item.getUnitPrice())
                    .quantity(item.getQuantity())
                    .build());
        }

        orderStatusHistoryRepository.save(OrderStatusHistory.builder()
                .order(order)
                .fromStatus(null)
                .toStatus(OrderStatus.PENDING.name())
                .note("Order placed")
                .changedAt(LocalDateTime.now())
                .build());

        log.info("Order saved - orderId: {}, items: {}, total: {}",
                order.getId(), items.size(), totalAmount);
        return order;
    }

    // payment.completed consumer path - only a PENDING order can become PAID
    @Override
    @Transactional
    public Order markPaid(Long orderId, String stripePaymentIntentId) {
        Order order = loadOrder(orderId);

        if (order.getStatus() != OrderStatus.PENDING) {
            log.warn("Ignoring payment.completed for order {} - current status: {} (not PENDING)",
                    orderId, order.getStatus());
            throw new InvalidStatusTransitionException(
                    "Order " + orderId + " is not awaiting payment (status: " + order.getStatus() + ")");
        }

        changeStatus(order, OrderStatus.PAID,
                "Payment confirmed" + (stripePaymentIntentId != null ? " - " + stripePaymentIntentId : ""));
        log.info("Order {} marked PAID after payment completion", orderId);
        return order;
    }

    // payment.failed consumer path - payment failed, order dies and stock goes back
    @Override
    @Transactional
    public Order markPaymentFailed(Long orderId, String failureReason) {
        Order order = loadOrder(orderId);

        if (order.getStatus() != OrderStatus.PENDING) {
            log.warn("Ignoring payment.failed for order {} - current status: {} (not PENDING)",
                    orderId, order.getStatus());
            throw new InvalidStatusTransitionException(
                    "Order " + orderId + " is not awaiting payment (status: " + order.getStatus() + ")");
        }

        changeStatus(order, OrderStatus.PAYMENT_FAILED,
                failureReason != null ? "Payment failed - " + failureReason : "Payment failed");

        // reserved units must go back to inventory
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        for (OrderItem item : items) {
            try {
                productClient.releaseStock(item.getProductId(), item.getQuantity());
            } catch (RuntimeException e) {
                log.error("STOCK RELEASE FAILED after payment failure - {} units of product {} stranded: {}",
                        item.getQuantity(), item.getProductId(), e.getMessage());
            }
        }
        log.info("Order {} marked PAYMENT_FAILED - {} item reservations released", orderId, items.size());
        return order;
    }

    // shared status-change helper: persist, write history row, publish event
    private void changeStatus(Order order, OrderStatus toStatus, String note) {
        String fromStatus = order.getStatus().name();

        order.setStatus(toStatus);
        orderRepository.save(order);

        orderStatusHistoryRepository.save(OrderStatusHistory.builder()
                .order(order)
                .fromStatus(fromStatus)
                .toStatus(toStatus.name())
                .note(note)
                .changedAt(LocalDateTime.now())
                .build());

        publishOrderStatusUpdated(order, fromStatus, toStatus.name(), note);
    }

    private Order loadOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
    }

    // 4.2 - paginated order history, newest first (composite index user_id + created_at)
    @Override
    public Page<Order> getUserOrders(Long userId, Pageable pageable) {
        return orderRepository.findByUserId(userId, pageable);
    }

    // 4.3 - ownership check: another user's order is 403, not 404
    @Override
    public Order getOrderForUser(Long orderId, Long userId) {
        Order order = loadOrder(orderId);
        if (!order.getUserId().equals(userId)) {
            log.warn("User {} denied access to order {} (owner: {})", userId, orderId, order.getUserId());
            throw new OrderAccessDeniedException("You do not have access to this order");
        }
        return order;
    }

    // 4.5 - admin fulfillment transitions per locked rules:
    // forward chain PENDING->PAID->PROCESSING->SHIPPED->DELIVERED,
    // CANCELLED only from PENDING/PAID
    @Override
    @Transactional
    public Order updateStatus(Long orderId, String newStatus, String note) {
        Order order = loadOrder(orderId);

        OrderStatus target;
        try {
            target = OrderStatus.valueOf(newStatus.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidStatusTransitionException("Unknown status: " + newStatus);
        }

        OrderStatus current = order.getStatus();
        if (!current.allowedAdminStatuses().contains(target)) {
            log.warn("Admin transition rejected for order {}: {} -> {}",
                    orderId, current, target);
            throw new InvalidStatusTransitionException(
                    "Cannot move order from " + current + " to " + target);
        }

        changeStatus(order, target,
                note != null && !note.isBlank() ? note : "Status updated by admin");
        log.info("Order {} status updated by admin: {} -> {}", orderId, current, target);
        return order;
    }

    private void publishOrderPlaced(Order order) {        try {
            List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
            OrderPlacedEvent event = OrderPlacedEvent.builder()
                    .orderId(order.getId())
                    .userId(order.getUserId())
                    .totalAmount(order.getTotalAmount())
                    .currency(order.getCurrency())
                    .items(items.stream()
                            .map(item -> OrderPlacedEvent.EventItem.builder()
                                    .productId(item.getProductId())
                                    .productName(item.getProductName())
                                    .unitPrice(item.getUnitPrice())
                                    .quantity(item.getQuantity())
                                    .build())
                            .toList())
                    .placedAt(LocalDateTime.now())
                    .build();
            orderPlacedKafkaTemplate.send(orderPlacedTopic, order.getId().toString(), event);
            log.info("Published order.placed event - orderId: {}", order.getId());
        } catch (Exception e) {
            // publishing must never break checkout - payment intent can be created on confirm
            log.error("Failed to publish order.placed event - orderId: {}: {}",
                    order.getId(), e.getMessage(), e);
        }
    }

    private void publishOrderStatusUpdated(Order order, String fromStatus, String toStatus, String note) {
        try {
            OrderStatusUpdatedEvent event = OrderStatusUpdatedEvent.builder()
                    .orderId(order.getId())
                    .userId(order.getUserId())
                    .fromStatus(fromStatus)
                    .toStatus(toStatus)
                    .note(note)
                    .updatedAt(LocalDateTime.now())
                    .build();
            orderStatusUpdatedKafkaTemplate.send(orderStatusUpdatedTopic, order.getId().toString(), event);
            log.info("Published order.status.updated event - orderId: {}, {} -> {}",
                    order.getId(), fromStatus, toStatus);
        } catch (Exception e) {
            // a missed notification is not worth failing a committed status change
            log.error("Failed to publish order.status.updated event - orderId: {}: {}",
                    order.getId(), e.getMessage(), e);
        }
    }

    // release every reservation, log-and-continue per item:
    // a partial rollback failure must not mask the original error
    private void rollbackReservations(List<CartItemData> reservedItems) {
        for (CartItemData item : reservedItems) {
            try {
                productClient.releaseStock(item.getProductId(), item.getQuantity());
            } catch (RuntimeException e) {
                log.error("ROLLBACK FAILED - {} units of product {} may be stranded: {}",
                        item.getQuantity(), item.getProductId(), e.getMessage());
            }
        }
    }
}
