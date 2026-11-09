package com.SwiftBasket.order_service.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.SwiftBasket.order_service.exception.InsufficientStockException;
import com.SwiftBasket.order_service.exception.ProductUnavailableException;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ProductClient {

    private final RestClient restClient;

    public ProductClient(@Value("${product.service.url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    // negative delta = reserve units for an order
    public void reserveStock(Long productId, int quantity) {
        log.info("Reserving {} units of product {} at product-service", quantity, productId);
        adjustStock(productId, -quantity);
    }

    // positive delta = release previously reserved units
    public void releaseStock(Long productId, int quantity) {
        log.info("Releasing {} units of product {} at product-service", quantity, productId);
        adjustStock(productId, quantity);
    }

    private void adjustStock(Long productId, int delta) {
        try {
            restClient.post()
                    .uri(uriBuilder -> uriBuilder.path("/api/v1/products/{id}/stock")
                            .queryParam("delta", delta)
                            .build(productId))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Stock adjusted for product {} - delta: {}", productId, delta);

        } catch (RestClientResponseException e) {
            HttpStatusCode status = e.getStatusCode();
            if (status.value() == 409) {
                // insufficient stock or optimistic-lock conflict at product-service
                log.warn("Stock adjustment rejected for product {} (delta {}) - status: {}",
                        productId, delta, status);
                throw new InsufficientStockException(
                        "Insufficient stock for product " + productId);
            }
            log.error("Product service rejected stock adjustment for product {} - status: {}",
                    productId, status);
            throw new ProductUnavailableException("Product service is currently unavailable");

        } catch (InsufficientStockException | ProductUnavailableException e) {
            throw e;

        } catch (Exception e) {
            // product-service unreachable or timed out
            log.error("Product service unavailable while adjusting stock for product {}: {}",
                    productId, e.getMessage());
            throw new ProductUnavailableException("Product service is currently unavailable");
        }
    }
}
