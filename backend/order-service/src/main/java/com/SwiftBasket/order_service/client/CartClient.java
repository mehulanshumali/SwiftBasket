package com.SwiftBasket.order_service.client;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.SwiftBasket.order_service.exception.CartServiceUnavailableException;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class CartClient {

    private final RestClient restClient;

    public CartClient(@Value("${cart.service.url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    // only the fields checkout needs from the cart
    @Data
    public static class CartData {
        private String cartId;
        private List<CartItemData> items;
        private BigDecimal totalPrice;
    }

    @Data
    public static class CartItemData {
        private Long productId;
        private String name;
        private BigDecimal unitPrice;
        private int quantity;
    }

    public CartData getCart(Long userId) {
        log.info("Fetching cart for user {} from cart-service", userId);
        try {
            CartData cart = restClient.get()
                    .uri("/api/v1/cart")
                    .header("X-User-ID", String.valueOf(userId))
                    .retrieve()
                    .body(CartData.class);

            if (cart == null) {
                log.warn("Cart response body was empty for user {}", userId);
                throw new CartServiceUnavailableException("Cart service returned an empty response");
            }
            log.info("Cart fetched for user {} - items: {}", userId, cart.getItems().size());
            return cart;

        } catch (RestClientResponseException e) {
            log.error("Cart service rejected request for user {} - status: {}", userId, e.getStatusCode());
            throw new CartServiceUnavailableException("Cart service is currently unavailable");

        } catch (CartServiceUnavailableException e) {
            throw e;

        } catch (Exception e) {
            // cart-service unreachable or timed out - checkout cannot proceed
            log.error("Cart service unavailable while fetching cart for user {}: {}", userId, e.getMessage());
            throw new CartServiceUnavailableException("Cart service is currently unavailable");
        }
    }

    public void clearCart(Long userId) {
        log.info("Clearing cart for user {} at cart-service", userId);
        try {
            restClient.delete()
                    .uri("/api/v1/cart")
                    .header("X-User-ID", String.valueOf(userId))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Cart cleared for user {}", userId);

        } catch (Exception e) {
            // order exists and stock is reserved - a stale cart is cosmetic, never fail checkout
            log.warn("Failed to clear cart for user {} after checkout: {}", userId, e.getMessage());
        }
    }
}
