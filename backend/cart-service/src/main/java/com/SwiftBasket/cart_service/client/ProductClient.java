package com.SwiftBasket.cart_service.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.SwiftBasket.cart_service.dto.ProductResponse;
import com.SwiftBasket.cart_service.exception.ProductNotAvailableException;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ProductClient {

    private final RestClient restClient;

    public ProductClient(@Value("${product.service.url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public ProductResponse getProduct(Long productId) {
        log.info("Fetching product {} from product-service", productId);
        try {
            ProductResponse product = restClient.get()
                    .uri("/api/v1/products/{id}", productId)
                    .retrieve()
                    .body(ProductResponse.class);

            if (product == null) {
                log.warn("Product {} response body was empty", productId);
                throw new ProductNotAvailableException("Product not available");
            }

            log.info("Product {} fetched: {}", productId, product.getName());
            return product;

        } catch (RestClientResponseException e) {
            // product-service answered but rejected the request (404 most likely)
            log.warn("Product {} not available - status: {}", productId, e.getStatusCode());
            throw new ProductNotAvailableException("Product not found");

        } catch (Exception e) {
            // product-service unreachable or timed out - fail cleanly, save nothing
            log.error("Product service unavailable while fetching product {}: {}", productId, e.getMessage());
            throw new ProductNotAvailableException("Product service is currently unavailable");
        }
    }
}
