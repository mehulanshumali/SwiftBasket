package com.SwiftBasket.payment_service.event;

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
@Schema(name = "PaymentFailedEvent", description = "Kafka event published when payment confirmation fails; order-service marks the order PAYMENT_FAILED and releases reserved stock")
public class PaymentFailedEvent {

    @Schema(description = "Internal payment record ID")
    private Long paymentId;

    @Schema(description = "Failed order's ID - also the Kafka message key")
    private Long orderId;

    @Schema(description = "Payer (claim-check: no email)")
    private Long userId;

    @Schema(description = "Amount that failed")
    private BigDecimal amount;

    @Schema(description = "3-letter ISO currency code (INR)")
    private String currency;

    @Schema(description = "Machine-readable failure reason, e.g. card_declined")
    private String failureReason;

    @Schema(description = "Failure timestamp")
    private LocalDateTime failedAt;
}
