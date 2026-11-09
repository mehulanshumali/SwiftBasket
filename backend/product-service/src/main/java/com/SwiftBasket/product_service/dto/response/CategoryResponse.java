package com.SwiftBasket.product_service.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "CategoryResponse", description = "Product category information")
public class CategoryResponse {

    @Schema(description = "Category's unique ID")
    private Long id;

    @Schema(description = "Category name")
    private String name;

    @Schema(description = "Category description")
    private String description;
}
