package com.SwiftBasket.payment_service.mapper;

import org.springframework.stereotype.Component;

import com.SwiftBasket.payment_service.client.StripeClient;
import com.SwiftBasket.payment_service.dto.PaymentIntentResponse;
import com.SwiftBasket.payment_service.dto.PaymentResponse;
import com.SwiftBasket.payment_service.dto.ReceiptResponse;
import com.SwiftBasket.payment_service.entity.Payment;

@Component
public class PaymentMapper {

    private final StripeClient stripeClient;

    public PaymentMapper(StripeClient stripeClient) {
        this.stripeClient = stripeClient;
    }

    public PaymentIntentResponse toIntentResponse(Payment payment) {
        return PaymentIntentResponse.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrderId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus().name())
                .clientSecret(null)
                .stripePaymentIntentId(payment.getStripePaymentIntentId())
                .testPaymentMethodId(stripeClient.getTestPaymentMethodId())
                .build();
    }

    public PaymentResponse toResponse(Payment payment) {
        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrderId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus().name())
                .stripePaymentIntentId(payment.getStripePaymentIntentId())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }

    public ReceiptResponse toReceiptResponse(Payment payment) {
        return ReceiptResponse.builder()
                .receiptNumber("R-" + payment.getId())
                .paymentId(payment.getId())
                .orderId(payment.getOrderId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .paidAt(payment.getUpdatedAt())
                .paymentMethod("Card (test)")
                .stripePaymentIntentId(payment.getStripePaymentIntentId())
                .build();
    }
}
