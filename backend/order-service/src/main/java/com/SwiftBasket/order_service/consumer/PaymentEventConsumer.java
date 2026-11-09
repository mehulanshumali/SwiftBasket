package com.SwiftBasket.order_service.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.SwiftBasket.order_service.event.PaymentCompletedEvent;
import com.SwiftBasket.order_service.event.PaymentFailedEvent;
import com.SwiftBasket.order_service.service.OrderService;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class PaymentEventConsumer {

    private final OrderService orderService;

    public PaymentEventConsumer(OrderService orderService) {
        this.orderService = orderService;
    }

    @KafkaListener(topics = "${kafka_topic.payment_completed}", groupId = "order-group")
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        log.info("Received payment.completed - paymentId: {}, orderId: {}",
                event.getPaymentId(), event.getOrderId());
        try {
            orderService.markPaid(event.getOrderId(), event.getStripePaymentIntentId());
        } catch (Exception e) {
            // log the failure; the transition guard in markPaid already
            // rejects stale/duplicate events, so anything here is systemic -
            // do NOT rethrow: rethrowing triggers endless redelivery of an
            // event that will never succeed
            log.error("Failed to process payment.completed - orderId: {}: {}",
                    event.getOrderId(), e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "${kafka_topic.payment_failed}", groupId = "order-group")
    public void onPaymentFailed(PaymentFailedEvent event) {
        log.info("Received payment.failed - paymentId: {}, orderId: {}",
                event.getPaymentId(), event.getOrderId());
        try {
            orderService.markPaymentFailed(event.getOrderId(), event.getFailureReason());
        } catch (Exception e) {
            log.error("Failed to process payment.failed - orderId: {}: {}",
                    event.getOrderId(), e.getMessage(), e);
        }
    }
}
