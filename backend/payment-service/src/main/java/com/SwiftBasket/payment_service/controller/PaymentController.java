package com.SwiftBasket.payment_service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.SwiftBasket.payment_service.dto.ConfirmPaymentRequest;
import com.SwiftBasket.payment_service.dto.PaymentIntentResponse;
import com.SwiftBasket.payment_service.dto.PaymentResponse;
import com.SwiftBasket.payment_service.dto.ReceiptResponse;
import com.SwiftBasket.payment_service.entity.Payment;
import com.SwiftBasket.payment_service.exception.UnauthorizedException;
import com.SwiftBasket.payment_service.mapper.PaymentMapper;
import com.SwiftBasket.payment_service.service.PaymentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments", description = "Stripe payment intents, confirmation, receipts and webhooks")
@Slf4j
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentMapper paymentMapper;

    @PostMapping("/orders/{orderId}/intent")
    @Operation(summary = "Create payment intent", description = "Creates a Stripe PaymentIntent for the order "
            + "(one payment per order enforced by UNIQUE constraint). Idempotent - returns the existing "
            + "intent if already created.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Payment intent created"),
        @ApiResponse(responseCode = "401", description = "Missing identity"),
        @ApiResponse(responseCode = "403", description = "Order belongs to another user"),
        @ApiResponse(responseCode = "409", description = "Order already paid"),
        @ApiResponse(responseCode = "503", description = "Stripe or order service unavailable")
    })
    public ResponseEntity<PaymentIntentResponse> createIntent(
            @RequestHeader(value = "X-User-ID", required = false) Long userId,
            @Parameter(description = "Order ID", required = true) @PathVariable("orderId") Long orderId) {

        requireIdentity(userId);
        log.info("Payment intent request - orderId: {}, userId: {}", orderId, userId);
        Payment payment = paymentService.createIntent(userId, orderId);

        return ResponseEntity.status(201).body(paymentMapper.toIntentResponse(payment));
    }

    @PostMapping("/orders/{orderId}/confirm")
    @Operation(summary = "Confirm payment", description = "Confirms the payment with a Stripe payment method "
            + "(test cards). On success publishes payment.completed; order-service flips the order to PAID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Payment confirmed"),
        @ApiResponse(responseCode = "401", description = "Missing identity"),
        @ApiResponse(responseCode = "403", description = "Order belongs to another user"),
        @ApiResponse(responseCode = "404", description = "No payment for order"),
        @ApiResponse(responseCode = "409", description = "Already paid or wrong state"),
        @ApiResponse(responseCode = "503", description = "Stripe unavailable")
    })
    public ResponseEntity<PaymentResponse> confirmPayment(
            @RequestHeader(value = "X-User-ID", required = false) Long userId,
            @Parameter(description = "Order ID", required = true) @PathVariable("orderId") Long orderId,
            @Parameter(description = "Payment method", required = true)
            @Valid @RequestBody ConfirmPaymentRequest request) {

        requireIdentity(userId);
        log.info("Payment confirm request - orderId: {}, userId: {}", orderId, userId);
        Payment payment = paymentService.confirm(userId, orderId, request.getPaymentMethodId());

        return ResponseEntity.ok(paymentMapper.toResponse(payment));
    }

    @GetMapping("/orders/{orderId}")
    @Operation(summary = "Payment status", description = "Payment status and metadata for the order.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Payment details"),
        @ApiResponse(responseCode = "401", description = "Missing identity"),
        @ApiResponse(responseCode = "403", description = "Order belongs to another user"),
        @ApiResponse(responseCode = "404", description = "No payment for order")
    })
    public ResponseEntity<PaymentResponse> getPayment(
            @RequestHeader(value = "X-User-ID", required = false) Long userId,
            @Parameter(description = "Order ID", required = true) @PathVariable("orderId") Long orderId) {

        requireIdentity(userId);
        log.info("Payment status request - orderId: {}, userId: {}", orderId, userId);
        Payment payment = paymentService.getByOrderId(userId, orderId);

        return ResponseEntity.ok(paymentMapper.toResponse(payment));
    }

    @GetMapping("/orders/{orderId}/receipt")
    @Operation(summary = "Payment receipt", description = "Receipt for a successful payment: receipt number, "
            + "amount, method, timestamp, Stripe reference.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Receipt"),
        @ApiResponse(responseCode = "401", description = "Missing identity"),
        @ApiResponse(responseCode = "403", description = "Order belongs to another user"),
        @ApiResponse(responseCode = "404", description = "No payment for order"),
        @ApiResponse(responseCode = "409", description = "Payment not succeeded yet")
    })
    public ResponseEntity<ReceiptResponse> getReceipt(
            @RequestHeader(value = "X-User-ID", required = false) Long userId,
            @Parameter(description = "Order ID", required = true) @PathVariable("orderId") Long orderId) {

        requireIdentity(userId);
        log.info("Receipt request - orderId: {}, userId: {}", orderId, userId);
        Payment payment = paymentService.getReceiptPayment(userId, orderId);

        return ResponseEntity.ok(paymentMapper.toReceiptResponse(payment));
    }

    @PostMapping(value = "/webhook/stripe", consumes = "application/json")
    @Operation(summary = "Stripe webhook", description = "Receives Stripe webhooks "
            + "(payment_intent.succeeded / payment_intent.payment_failed); verifies the Stripe-Signature "
            + "header, logs the event, keeps payment status in sync.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Webhook processed"),
        @ApiResponse(responseCode = "400", description = "Invalid signature")
    })
    public ResponseEntity<Void> handleStripeWebhook(
            @Parameter(description = "Raw webhook payload", required = true) @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String signatureHeader) {

        log.info("Stripe webhook received");
        paymentService.handleWebhookEvent(payload, signatureHeader);
        return ResponseEntity.ok().build();
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
}
