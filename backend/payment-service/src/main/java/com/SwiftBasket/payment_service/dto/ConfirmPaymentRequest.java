package com.SwiftBasket.payment_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(name = "ConfirmPaymentRequest", description = "Payment confirmation request with a Stripe payment method")
public class ConfirmPaymentRequest {

    @Schema(description = "Stripe payment method token, e.g. pm_card_visa (test)")
    @NotBlank(message = "Payment method ID is required")
    private String paymentMethodId;
}
