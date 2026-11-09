package com.SwiftBasket.payment_service.dto;

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
@Schema(name = "PaymentResponse", description = "Payment status and metadata")
public class PaymentResponse {

    @Schema(description = "Internal payment record ID")
    private Long paymentId;

    @Schema(description = "Order this payment belongs to")
    private Long orderId;

    @Schema(description = "Charged amount")
    private BigDecimal amount;

    @Schema(description = "3-letter ISO currency code (INR)")
    private String currency;

    @Schema(description = "Current payment status")
    private String status;

    @Schema(description = "Stripe PaymentIntent reference")
    private String stripePaymentIntentId;

    @Schema(description = "Failure reason, present when status is FAILED")
    private String failureReason;

    @Schema(description = "Payment creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedAt;
}
