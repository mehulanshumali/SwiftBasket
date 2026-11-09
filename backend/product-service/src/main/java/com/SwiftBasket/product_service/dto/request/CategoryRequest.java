package com.SwiftBasket.product_service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(name = "CategoryRequest", description = "Create or update category request")
public class CategoryRequest {

    @Schema(description = "Category name")
    @NotBlank(message = "Category name is required")
    @Size(min = 3, max = 100, message = "Category name must be between 3 and 100 characters")
    private String name;

    @Schema(description = "Category description")
    @Size(max = 500, message = "Category description cannot exceed 500 characters")
    private String description;
}
