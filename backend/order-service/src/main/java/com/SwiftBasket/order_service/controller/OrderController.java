package com.SwiftBasket.order_service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.SwiftBasket.order_service.dto.CheckoutRequest;
import com.SwiftBasket.order_service.dto.OrderResponse;
import com.SwiftBasket.order_service.dto.PagedResponse;
import com.SwiftBasket.order_service.dto.TrackingResponse;
import com.SwiftBasket.order_service.dto.UpdateStatusRequest;
import com.SwiftBasket.order_service.entity.Order;
import com.SwiftBasket.order_service.entity.OrderItem;
import com.SwiftBasket.order_service.entity.OrderStatusHistory;
import com.SwiftBasket.order_service.exception.AdminAccessException;
import com.SwiftBasket.order_service.exception.UnauthorizedException;
import com.SwiftBasket.order_service.mapper.OrderMapper;
import com.SwiftBasket.order_service.repository.OrderItemRepository;
import com.SwiftBasket.order_service.repository.OrderStatusHistoryRepository;
import com.SwiftBasket.order_service.service.OrderService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Orders", description = "Checkout, order history, tracking and admin fulfillment")
@Slf4j
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderStatusHistoryRepository orderStatusHistoryRepository;

    @PostMapping
    @Operation(summary = "Checkout", description = "Fetches the cart, reserves stock at the product service, "
            + "snapshots line items, creates a PENDING order, publishes order.placed and clears the cart.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Order created"),
        @ApiResponse(responseCode = "400", description = "Cart is empty or validation failed"),
        @ApiResponse(responseCode = "401", description = "Missing identity"),
        @ApiResponse(responseCode = "409", description = "Insufficient stock"),
        @ApiResponse(responseCode = "503", description = "Cart or product service unavailable")
    })
    public ResponseEntity<OrderResponse> checkout(
            @RequestHeader(value = "X-User-ID", required = false) Long userId,
            @Parameter(description = "Shipping details", required = true)
            @Valid @RequestBody CheckoutRequest request) {

        requireIdentity(userId);
        log.info("Checkout request from userId: {}", userId);
        Order order = orderService.checkout(userId, request);

        OrderResponse response = orderMapper.toResponse(order,
                orderItemRepository.findByOrderId(order.getId()));
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping
    @Operation(summary = "My orders", description = "Paginated order history for the current user, newest first.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Order history page"),
        @ApiResponse(responseCode = "401", description = "Missing identity")
    })
    public ResponseEntity<PagedResponse<OrderResponse>> getUserOrders(
            @RequestHeader(value = "X-User-ID", required = false) Long userId,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Items per page") @RequestParam(defaultValue = "10") int size) {

        requireIdentity(userId);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Order> orders = orderService.getUserOrders(userId, pageable);

        PagedResponse<OrderResponse> response = PagedResponse.<OrderResponse>builder()
                .content(orders.getContent().stream()
                        .map(order -> orderMapper.toResponse(order,
                                orderItemRepository.findByOrderId(order.getId())))
                        .toList())
                .pageNumber(orders.getNumber())
                .pageSize(orders.getSize())
                .totalElements(orders.getTotalElements())
                .totalPages(orders.getTotalPages())
                .last(orders.isLast())
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Order details", description = "Full order details with line items. "
            + "Returns 403 if the order belongs to another user.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Order details"),
        @ApiResponse(responseCode = "401", description = "Missing identity"),
        @ApiResponse(responseCode = "403", description = "Order belongs to another user"),
        @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<OrderResponse> getOrder(
            @RequestHeader(value = "X-User-ID", required = false) Long userId,
            @Parameter(description = "Order ID", required = true) @PathVariable("id") Long id) {

        requireIdentity(userId);
        log.info("Order details request - orderId: {}, userId: {}", id, userId);
        Order order = orderService.getOrderForUser(id, userId);

        return ResponseEntity.ok(orderMapper.toResponse(order,
                orderItemRepository.findByOrderId(order.getId())));
    }

    @GetMapping("/{id}/tracking")
    @Operation(summary = "Track order", description = "Delivery status timeline from order status history.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Status timeline"),
        @ApiResponse(responseCode = "401", description = "Missing identity"),
        @ApiResponse(responseCode = "403", description = "Order belongs to another user"),
        @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<TrackingResponse> trackOrder(
            @RequestHeader(value = "X-User-ID", required = false) Long userId,
            @Parameter(description = "Order ID", required = true) @PathVariable("id") Long id) {

        requireIdentity(userId);
        log.info("Tracking request - orderId: {}, userId: {}", id, userId);
        Order order = orderService.getOrderForUser(id, userId);

        return ResponseEntity.ok(orderMapper.toTrackingResponse(order,
                orderStatusHistoryRepository.findByOrderIdOrderByChangedAtAsc(order.getId())));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update order status (admin)", description = "Advances fulfillment status: "
            + "PENDING->PAID->PROCESSING->SHIPPED->DELIVERED; CANCELLED only from PENDING/PAID. "
            + "Appends to status history and publishes order.status.updated.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Status updated"),
        @ApiResponse(responseCode = "400", description = "Invalid transition or unknown status"),
        @ApiResponse(responseCode = "401", description = "Missing identity"),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<OrderResponse> updateStatus(
            @RequestHeader(value = "X-User-ID", required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @Parameter(description = "Order ID", required = true) @PathVariable("id") Long id,
            @Parameter(description = "New status and optional note", required = true)
            @Valid @RequestBody UpdateStatusRequest request) {

        requireIdentity(userId);
        requireAdmin(userRole);
        log.info("Admin status update - orderId: {}, newStatus: {}", id, request.getStatus());
        Order order = orderService.updateStatus(id, request.getStatus(), request.getNote());

        return ResponseEntity.ok(orderMapper.toResponse(order,
                orderItemRepository.findByOrderId(order.getId())));
    }

    @GetMapping("/health")
    @Operation(summary = "Liveness probe", description = "Returns 200 when the service is up.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Service is healthy")
    })
    public ResponseEntity<Void> health() {
        return ResponseEntity.ok().build();
    }

    private void requireIdentity(Long userId) {
        if (userId == null) {
            throw new UnauthorizedException("Missing X-User-ID header");
        }
    }

    private void requireAdmin(String userRole) {
        if (userRole == null || !userRole.trim().equalsIgnoreCase("ADMIN")) {
            throw new AdminAccessException("Admin role required");
        }
    }
}
