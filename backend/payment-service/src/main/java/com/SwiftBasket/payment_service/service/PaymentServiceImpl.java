package com.SwiftBasket.payment_service.service;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.SwiftBasket.payment_service.client.OrderClient;
import com.SwiftBasket.payment_service.client.StripeClient;
import com.SwiftBasket.payment_service.entity.Payment;
import com.SwiftBasket.payment_service.entity.PaymentEventLog;
import com.SwiftBasket.payment_service.entity.PaymentStatus;
import com.SwiftBasket.payment_service.event.PaymentCompletedEvent;
import com.SwiftBasket.payment_service.event.PaymentFailedEvent;
import com.SwiftBasket.payment_service.exception.DuplicatePaymentException;
import com.SwiftBasket.payment_service.exception.InvalidPaymentStateException;
import com.SwiftBasket.payment_service.exception.PaymentAccessDeniedException;
import com.SwiftBasket.payment_service.exception.PaymentNotFoundException;
import com.SwiftBasket.payment_service.exception.WebhookSignatureException;
import com.SwiftBasket.payment_service.repository.PaymentEventLogRepository;
import com.SwiftBasket.payment_service.repository.PaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentEventLogRepository paymentEventLogRepository;

    @Autowired
    private StripeClient stripeClient;

    @Autowired
    private OrderClient orderClient;

    @Autowired
    private KafkaTemplate<String, PaymentCompletedEvent> paymentCompletedKafkaTemplate;

    @Autowired
    private KafkaTemplate<String, PaymentFailedEvent> paymentFailedKafkaTemplate;

    @Value("${kafka_topic.payment_completed}")
    private String paymentCompletedTopic;

    @Value("${kafka_topic.payment_failed}")
    private String paymentFailedTopic;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    // shared creation path: fetch the order once (amount, currency, owner),
    // create the Stripe intent, persist the payment row
    @Override
    @Transactional
    public Payment createIntent(Long userId, Long orderId) {
        Payment existing = paymentRepository.findByOrderId(orderId).orElse(null);
        if (existing != null) {
            if (existing.getStatus() == PaymentStatus.SUCCEEDED) {
                throw new DuplicatePaymentException("Order " + orderId + " has already been paid");
            }
            log.info("Payment intent already exists for order {} - returning existing (status: {})",
                    orderId, existing.getStatus());
            return existing;
        }

        return createPaymentForOrder(userId, orderId);
    }

    // consumer path: order.placed -> PaymentIntent + REQUIRES_PAYMENT row
    // idempotent via UNIQUE(order_id) - a duplicate event cannot create a second payment
    @Transactional
    public Payment createPaymentForOrder(Long userId, Long orderId) {
        Payment existing = paymentRepository.findByOrderId(orderId).orElse(null);
        if (existing != null) {
            log.info("Payment for order {} already exists (duplicate order.placed?) - skipping", orderId);
            return existing;
        }

        // single order lookup: owner, amount and currency in one call
        OrderClient.OrderData order = orderClient.getOrder(orderId, userId);
        if (!order.getUserId().equals(userId)) {
            log.warn("User {} tried to pay for order {} owned by {}", userId, orderId, order.getUserId());
            throw new PaymentAccessDeniedException("You do not have access to this order");
        }

        PaymentIntent intent = stripeClient.createPaymentIntent(
                order.getTotalAmount(), order.getCurrency());

        Payment payment = paymentRepository.save(Payment.builder()
                .orderId(orderId)
                .userId(userId)
                .amount(order.getTotalAmount())
                .currency(order.getCurrency())
                .status(PaymentStatus.REQUIRES_PAYMENT)
                .stripePaymentIntentId(intent.getId())
                .build());

        logEvent(payment, "payment.intent.created",
                "PaymentIntent " + intent.getId() + " created for order " + orderId);
        log.info("Payment created for order {} - paymentId: {}, intent: {}", orderId, payment.getId(), intent.getId());
        return payment;
    }

    // 5.2 - confirm with Stripe; publish payment.completed / payment.failed
    @Override
    @Transactional
    public Payment confirm(Long userId, Long orderId, String paymentMethodId) {
        Payment payment = getOwnedPayment(userId, orderId);

        if (payment.getStatus() == PaymentStatus.SUCCEEDED) {
            throw new DuplicatePaymentException("Order " + orderId + " has already been paid");
        }
        if (payment.getStatus() != PaymentStatus.REQUIRES_PAYMENT) {
            throw new InvalidPaymentStateException(
                    "Payment for order " + orderId + " cannot be confirmed (status: " + payment.getStatus() + ")");
        }

        PaymentIntent intent = stripeClient.confirmIntent(payment.getStripePaymentIntentId(), paymentMethodId);
        String intentStatus = intent.getStatus();
        log.info("Stripe confirmation result for order {} - intent status: {}", orderId, intentStatus);

        if ("succeeded".equals(intentStatus)) {
            payment.setStatus(PaymentStatus.SUCCEEDED);
            payment = paymentRepository.save(payment);
            logEvent(payment, "payment.succeeded", "PaymentIntent " + intent.getId() + " succeeded");
            publishPaymentCompleted(payment);
        } else if ("processing".equals(intentStatus)) {
            payment.setStatus(PaymentStatus.PROCESSING);
            payment = paymentRepository.save(payment);
            logEvent(payment, "payment.processing", "PaymentIntent " + intent.getId() + " processing");
        } else {
            // requires_payment_method / canceled / etc - treat as failed
            payment.setStatus(PaymentStatus.FAILED);
            payment = paymentRepository.save(payment);
            String reason = intent.getLastPaymentError() != null
                    ? intent.getLastPaymentError().getCode()
                    : "payment_failed";
            logEvent(payment, "payment.failed", "PaymentIntent " + intent.getId() + " failed: " + reason);
            publishPaymentFailed(payment, reason);
        }
        return payment;
    }

    // 5.3 - payment status and metadata
    @Override
    public Payment getByOrderId(Long userId, Long orderId) {
        return getOwnedPayment(userId, orderId);
    }

    // 5.4 - receipt is only meaningful for a succeeded payment
    @Override
    public Payment getReceiptPayment(Long userId, Long orderId) {
        Payment payment = getOwnedPayment(userId, orderId);
        if (payment.getStatus() != PaymentStatus.SUCCEEDED) {
            throw new InvalidPaymentStateException(
                    "No receipt available - payment for order " + orderId + " is " + payment.getStatus());
        }
        return payment;
    }

    // 5.5 - signature-verified Stripe webhook; keeps status in sync, idempotent
    @Override
    public void handleWebhookEvent(String payload, String signatureHeader) {
        Event event;
        try {
            event = Webhook.constructEvent(payload, signatureHeader, webhookSecret);
        } catch (Exception e) {
            log.warn("Stripe webhook signature verification failed: {}", e.getMessage());
            throw new WebhookSignatureException("Invalid webhook signature");
        }

        log.info("Stripe webhook received - type: {}, id: {}", event.getType(), event.getId());

        if (!"payment_intent.succeeded".equals(event.getType())
                && !"payment_intent.payment_failed".equals(event.getType())) {
            log.info("Webhook event type {} ignored", event.getType());
            return;
        }

        PaymentIntent deserialized;
        try {
            deserialized = event.getDataObjectDeserializer().getObject().isPresent()
                    ? (PaymentIntent) event.getDataObjectDeserializer().getObject().get()
                    : null;
        } catch (Exception e) {
            log.error("Failed to deserialize webhook PaymentIntent: {}", e.getMessage());
            return;
        }
        if (deserialized == null) {
            log.warn("Webhook carried no deserializable PaymentIntent - logging only");
            return;
        }

        final PaymentIntent intent = deserialized;
        final String eventId = event.getId();
        final String eventType = event.getType();
        final String failureReason = intent.getLastPaymentError() != null
                ? intent.getLastPaymentError().getCode()
                : "payment_failed";

        paymentRepository.findAll().stream()
                .filter(p -> intent.getId().equals(p.getStripePaymentIntentId()))
                .findFirst()
                .ifPresentOrElse(payment -> {
                    // only move forward - a webhook is a late echo, never a regression
                    if (payment.getStatus() == PaymentStatus.SUCCEEDED) {
                        log.info("Webhook for intent {} - payment already SUCCEEDED, ignoring", intent.getId());
                        return;
                    }
                    if ("payment_intent.succeeded".equals(eventType)) {
                        payment.setStatus(PaymentStatus.SUCCEEDED);
                        paymentRepository.save(payment);
                        logEvent(payment, "webhook.payment_intent.succeeded", "Synced from webhook " + eventId);
                        publishPaymentCompleted(payment);
                    } else {
                        payment.setStatus(PaymentStatus.FAILED);
                        paymentRepository.save(payment);
                        logEvent(payment, "webhook.payment_intent.payment_failed",
                                "Synced from webhook " + eventId + ": " + failureReason);
                        publishPaymentFailed(payment, failureReason);
                    }
                }, () -> log.warn("Webhook for unknown intent {} - no local payment", intent.getId()));
    }

    // ownership check: another user's payment is 403, not 404
    private Payment getOwnedPayment(Long userId, Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentNotFoundException("No payment found for order " + orderId));
        if (!payment.getUserId().equals(userId)) {
            log.warn("User {} denied access to payment for order {} (owner: {})",
                    userId, orderId, payment.getUserId());
            throw new PaymentAccessDeniedException("You do not have access to this payment");
        }
        return payment;
    }

    private void publishPaymentCompleted(Payment payment) {
        try {
            PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                    .paymentId(payment.getId())
                    .orderId(payment.getOrderId())
                    .userId(payment.getUserId())
                    .amount(payment.getAmount())
                    .currency(payment.getCurrency())
                    .stripePaymentIntentId(payment.getStripePaymentIntentId())
                    .completedAt(LocalDateTime.now())
                    .build();
            paymentCompletedKafkaTemplate.send(paymentCompletedTopic, payment.getOrderId().toString(), event);
            log.info("Published payment.completed - orderId: {}", payment.getOrderId());
        } catch (Exception e) {
            // the webhook is the safety net - a missed publish must not corrupt the payment
            log.error("Failed to publish payment.completed - orderId: {}: {}",
                    payment.getOrderId(), e.getMessage(), e);
        }
    }

    private void publishPaymentFailed(Payment payment, String reason) {
        try {
            PaymentFailedEvent event = PaymentFailedEvent.builder()
                    .paymentId(payment.getId())
                    .orderId(payment.getOrderId())
                    .userId(payment.getUserId())
                    .amount(payment.getAmount())
                    .currency(payment.getCurrency())
                    .failureReason(reason)
                    .failedAt(LocalDateTime.now())
                    .build();
            paymentFailedKafkaTemplate.send(paymentFailedTopic, payment.getOrderId().toString(), event);
            log.info("Published payment.failed - orderId: {}", payment.getOrderId());
        } catch (Exception e) {
            log.error("Failed to publish payment.failed - orderId: {}: {}",
                    payment.getOrderId(), e.getMessage(), e);
        }
    }

    private static final ObjectMapper JSON = new ObjectMapper();

    private void logEvent(Payment payment, String eventType, String description) {
        try {
            paymentEventLogRepository.save(PaymentEventLog.builder()
                    .payment(payment)
                    .eventType(eventType)
                    // payload column is MySQL JSON
                    .payload(JSON.writeValueAsString(Map.of("message", description)))
                    .createdAt(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            // event log is an audit aid - never block the payment path
            log.error("Failed to write payment event log - paymentId: {}: {}", payment.getId(), e.getMessage());
        }
    }
}
