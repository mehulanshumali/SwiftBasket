package com.SwiftBasket.cart_service.document;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data 
@Builder 
@NoArgsConstructor 
@AllArgsConstructor 
public class CartItem {
    private Long productId;
    private String name;
    private String imageUrl;
    private BigDecimal unitPrice;
    private int quantity;
}
