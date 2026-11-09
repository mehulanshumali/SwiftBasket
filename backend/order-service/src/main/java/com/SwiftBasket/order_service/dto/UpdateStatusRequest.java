package com.SwiftBasket.order_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(name = "UpdateStatusRequest", description = "Admin request to advance an order's fulfillment status")
public class UpdateStatusRequest {

    @Schema(description = "New status to transition the order to")
    @NotBlank(message = "Status is required")
    private String status;

    @Schema(description = "Optional note, e.g. courier AWB number")
    @Size(max = 500, message = "Note cannot exceed 500 characters")
    private String note;
}
