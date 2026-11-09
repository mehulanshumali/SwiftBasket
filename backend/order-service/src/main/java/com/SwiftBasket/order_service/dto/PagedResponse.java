package com.SwiftBasket.order_service.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "PagedResponse", description = "Paginated list of results")
public class PagedResponse<T> {

    @Schema(description = "The actual list of results on this page")
    private List<T> content;

    @Schema(description = "Current page number (0-based)")
    private int pageNumber;

    @Schema(description = "Number of items per page")
    private int pageSize;

    @Schema(description = "Total items across all pages")
    private long totalElements;

    @Schema(description = "Total number of pages")
    private int totalPages;

    @Schema(description = "Whether this is the last page")
    private boolean last;
}
