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
@Schema(name = "ReceiptResponse", description = "Receipt for a successful payment")
public class ReceiptResponse {

    @Schema(description = "Human-readable receipt number, e.g. R-900")
    private String receiptNumber;

    @Schema(description = "Internal payment record ID")
    private Long paymentId;

    @Schema(description = "Order this payment belongs to")
    private Long orderId;

    @Schema(description = "Charged amount")
    private BigDecimal amount;

    @Schema(description = "3-letter ISO currency code (INR)")
    private String currency;

    @Schema(description = "Timestamp of successful payment")
    private LocalDateTime paidAt;

    @Schema(description = "Payment method description, e.g. Visa card (test)")
    private String paymentMethod;

    @Schema(description = "Stripe PaymentIntent reference")
    private String stripePaymentIntentId;
}
