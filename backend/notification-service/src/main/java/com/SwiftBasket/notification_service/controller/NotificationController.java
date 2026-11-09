package com.SwiftBasket.notification_service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.SwiftBasket.notification_service.client.UserClient;
import com.SwiftBasket.notification_service.dto.NotificationResponse;
import com.SwiftBasket.notification_service.entity.Notification;
import com.SwiftBasket.notification_service.exception.GlobalExceptionHandler.ErrorResponse;
import com.SwiftBasket.notification_service.exception.MissingIdentityException;
import com.SwiftBasket.notification_service.exception.UserEmailResolutionException;
import com.SwiftBasket.notification_service.mapper.NotificationMapper;
import com.SwiftBasket.notification_service.repository.NotificationRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/notifications")
@Slf4j
@Tag(name = "Notifications", description = "Notification audit trail - users can list their own notification history")
public class NotificationController {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserClient userClient;

    @Autowired
    private NotificationMapper notificationMapper;

    @GetMapping
    @Operation(summary = "List my notifications", description = "Returns a page of the caller's own notification "
            + "records (emails sent to their current address), newest first. Identity comes from the X-User-ID "
            + "header set by the Kong JWT plugin - no self-service for other users' inboxes.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Page of the caller's notifications"),
        @ApiResponse(responseCode = "401", description = "Missing X-User-ID header (not called via Kong or unauthenticated)"),
        @ApiResponse(responseCode = "503", description = "User service unavailable - email could not be resolved")
    })
    public ResponseEntity<Page<NotificationResponse>> getMyNotifications(
            @Parameter(description = "User ID injected by the Kong JWT plugin", required = true)
            @RequestHeader(value = "X-User-ID", required = false) Long userId,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {

        if (userId == null) {
            log.warn("Notifications listing attempted without X-User-ID header");
            throw new MissingIdentityException("Authentication required: missing X-User-ID header");
        }

        String email = userClient.getEmail(userId);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Notification> notifications = notificationRepository
                .findByRecipientEmailOrderByCreatedAtDesc(email, pageable);

        log.info("Listing {} notifications for userId {}", notifications.getNumberOfElements(), userId);
        return ResponseEntity.ok(notifications.map(notificationMapper::toResponse));
    }

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Simple health check endpoint to verify service is running.")
    @ApiResponse(responseCode = "200", description = "Service is healthy")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Notification Service is running");
    }
}
