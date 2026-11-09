package com.SwiftBasket.cart_service.document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Document("carts")
@Data
@Builder 
@NoArgsConstructor 
@AllArgsConstructor 
public class Cart {
    
    @Id 
    private String id;

    @Indexed(unique = true, sparse = true)
    private Long userId;

    @Indexed(unique = true, sparse = true)
    private String sessionId;

    @Builder.Default
    private List<CartItem> items = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public BigDecimal getTotalPrice() {
        return items.stream()
                .map(item -> item.getUnitPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

}
