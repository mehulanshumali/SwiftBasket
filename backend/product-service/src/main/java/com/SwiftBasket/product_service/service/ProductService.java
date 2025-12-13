package com.SwiftBasket.product_service.service;

import com.SwiftBasket.product_service.dto.response.PagedResponse;
import com.SwiftBasket.product_service.dto.response.ProductResponse;
import com.SwiftBasket.product_service.dto.response.ProductSearchResponse;

public interface ProductService {
    PagedResponse<ProductResponse> getAllProducts(int page, int size);
    PagedResponse<ProductResponse> getProductsByCategory(Long categoryId, int page, int size);
    ProductResponse getProductById(Long id);
    PagedResponse<ProductSearchResponse> searchProducts(String query, int page, int size);
}
