package com.SwiftBasket.order_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(name = "CheckoutRequest", description = "Checkout request with shipping details")
public class CheckoutRequest {

    @Schema(description = "Shipping address for the order")
    @NotBlank(message = "Shipping address is required")
    @Size(max = 500, message = "Shipping address cannot exceed 500 characters")
    private String shippingAddress;
}
