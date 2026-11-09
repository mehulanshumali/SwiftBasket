package com.SwiftBasket.notification_service.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "PaymentCompletedEvent", description = "Kafka event from payment-service; triggers the payment receipt email")
public class PaymentCompletedEvent {

    @Schema(description = "Internal payment record ID")
    private Long paymentId;

    @Schema(description = "Paid order's ID")
    private Long orderId;

    @Schema(description = "Payer (claim-check: email resolved at send time)")
    private Long userId;

    @Schema(description = "Amount charged (must match order total)")
    private BigDecimal amount;

    @Schema(description = "3-letter ISO currency code (INR)")
    private String currency;

    @Schema(description = "Stripe PaymentIntent reference for support and refunds")
    private String stripePaymentIntentId;

    @Schema(description = "Confirmation timestamp")
    private LocalDateTime completedAt;
}
