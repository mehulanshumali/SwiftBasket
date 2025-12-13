package com.SwiftBasket.product_service.dto.response;

import java.util.List;

import lombok.Data;

@Data
public class PagedResponse<T> {
    private List<T> content;      // The actual list of Products/Categories
    private int pageNumber;       // Current page (e.g., 0)
    private int pageSize;         // Items per page (e.g., 20)
    private long totalElements;   // Total items in DB (e.g., 500)
    private int totalPages;       // Total pages calculated (e.g., 25)
    private boolean last;         // Is this the last page?
}