package com.SwiftBasket.cart_service.event;

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
@Schema(name = "CartItemAddedEvent", description = "Kafka event published when an item is added to a cart; reserved for future analytics")
public class CartItemAddedEvent {

    @Schema(description = "Mongo ObjectId of the cart the item was added to")
    private String cartId;

    @Schema(description = "Owning user - also the Kafka message key (sessionId key for guest carts); null for guest carts")
    private Long userId;

    @Schema(description = "Product added")
    private Long productId;

    @Schema(description = "Product name snapshot at add time")
    private String productName;

    @Schema(description = "Quantity added in this action")
    private Integer quantity;

    @Schema(description = "Unit price snapshot at add time")
    private BigDecimal unitPrice;

    @Schema(description = "Add-to-cart timestamp")
    private LocalDateTime addedAt;
}
