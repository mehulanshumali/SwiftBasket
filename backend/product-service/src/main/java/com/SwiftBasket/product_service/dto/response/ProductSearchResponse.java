package com.SwiftBasket.product_service.dto.response;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSearchResponse {
    
    private String id;
    private String name;
    private String description;
    private BigDecimal price;
    private String categoryName;
    private String imageUrl;
    private boolean active;

}
