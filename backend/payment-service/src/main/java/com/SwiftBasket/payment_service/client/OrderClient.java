package com.SwiftBasket.payment_service.client;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.SwiftBasket.payment_service.exception.OrderServiceUnavailableException;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class OrderClient {

    private final RestClient restClient;

    public OrderClient(@Value("${order.service.url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    // minimal projection of order-service's OrderResponse - only what payment needs
    @Data
    public static class OrderData {
        private Long orderId;
        private String status;
        private BigDecimal totalAmount;
        private String currency;
        private Long userId;
    }

    public OrderData getOrder(Long orderId, Long userId) {
        log.info("Fetching order {} (user {}) from order-service", orderId, userId);
        try {
            OrderData order = restClient.get()
                    .uri("/api/v1/orders/{id}", orderId)
                    .header("X-User-ID", String.valueOf(userId))
                    .retrieve()
                    .body(OrderData.class);

            if (order == null) {
                log.warn("Order response body was empty for orderId {}", orderId);
                throw new OrderServiceUnavailableException("Order service returned an empty response");
            }
            return order;

        } catch (RestClientResponseException e) {
            log.error("Order service rejected request for order {} - status: {}", orderId, e.getStatusCode());
            throw new OrderServiceUnavailableException("Order service is currently unavailable");

        } catch (OrderServiceUnavailableException e) {
            throw e;

        } catch (Exception e) {
            log.error("Order service unavailable while fetching order {}: {}", orderId, e.getMessage());
            throw new OrderServiceUnavailableException("Order service is currently unavailable");
        }
    }

    public BigDecimal getOrderAmount(Long orderId) {
        return getOrder(orderId).getTotalAmount();
    }

    public String getOrderCurrency(Long orderId) {
        return getOrder(orderId).getCurrency();
    }
}
