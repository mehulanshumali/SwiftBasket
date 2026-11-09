package com.SwiftBasket.product_service.service;

import com.SwiftBasket.product_service.dto.request.ProductRequest;
import com.SwiftBasket.product_service.dto.response.PagedResponse;
import com.SwiftBasket.product_service.dto.response.ProductResponse;
import com.SwiftBasket.product_service.dto.response.ProductSearchResponse;
import com.SwiftBasket.product_service.entity.Product;

public interface ProductService {
    PagedResponse<ProductResponse> getAllProducts(int page, int size);
    PagedResponse<ProductResponse> getProductsByCategory(Long categoryId, int page, int size);
    ProductResponse getProductById(Long productId);
    PagedResponse<ProductSearchResponse> searchProducts(String query, int page, int size);
    Product createProduct(ProductRequest request);
    Product updateProduct(Long productId, ProductRequest request);
    void deleteProduct(Long productId);
    Product updateStock(Long productId, int delta);
    int reindexAll();
}
