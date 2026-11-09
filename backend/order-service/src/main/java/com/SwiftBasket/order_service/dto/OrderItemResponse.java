package com.SwiftBasket.order_service.dto;

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
@Schema(name = "OrderItemResponse", description = "A single product line in an order")
public class OrderItemResponse {

    @Schema(description = "Product's unique ID")
    private Long productId;

    @Schema(description = "Product name captured at purchase time")
    private String productName;

    @Schema(description = "Unit price captured at purchase time")
    private BigDecimal unitPrice;

    @Schema(description = "Quantity ordered")
    private int quantity;
}
