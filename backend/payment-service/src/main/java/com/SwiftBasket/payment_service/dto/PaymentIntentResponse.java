package com.SwiftBasket.payment_service.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "PaymentIntentResponse", description = "Stripe payment intent and available checkout options")
public class PaymentIntentResponse {

    @Schema(description = "Internal payment record ID")
    private Long paymentId;

    @Schema(description = "Order this payment belongs to")
    private Long orderId;

    @Schema(description = "Amount to be charged (must match order total)")
    private BigDecimal amount;

    @Schema(description = "3-letter ISO currency code")
    private String currency;

    @Schema(description = "Current payment status")
    private String status;

    @Schema(description = "Stripe client secret - used by the browser-side Stripe SDK")
    private String clientSecret;

    @Schema(description = "Stripe PaymentIntent reference")
    private String stripePaymentIntentId;

    @Schema(description = "Configured payment method ID, available only with Stripe test credentials")
    private String testPaymentMethodId;
}
