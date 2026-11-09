package com.SwiftBasket.cart_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "AddItemRequest", description = "Add product to cart request")
public class AddItemRequest {

    @Schema(description = "ID of the product to add")
    @NotNull(message = "Product ID is required")
    private Long productId;

    @Schema(description = "Quantity to add (minimum 1)")
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
}
