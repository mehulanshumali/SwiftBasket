package com.SwiftBasket.payment_service.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.SwiftBasket.payment_service.event.OrderPlacedEvent;
import com.SwiftBasket.payment_service.service.PaymentService;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class OrderPlacedConsumer {

    private final PaymentService paymentService;

    public OrderPlacedConsumer(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @KafkaListener(topics = "${kafka_topic.order_placed}", groupId = "payment-group")
    public void onOrderPlaced(OrderPlacedEvent event) {
        log.info("Received order.placed - orderId: {}, userId: {}, total: {} {}",
                event.getOrderId(), event.getUserId(), event.getTotalAmount(), event.getCurrency());
        try {
            paymentService.createIntent(event.getUserId(), event.getOrderId());
        } catch (Exception e) {
            // createIntent is idempotent (UNIQUE order_id) and the REST fallback
            // path exists - log and move on rather than redelivering forever
            log.error("Failed to process order.placed - orderId: {}: {}",
                    event.getOrderId(), e.getMessage(), e);
        }
    }
}
