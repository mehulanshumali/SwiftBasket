package com.SwiftBasket.cart_service.service;

import java.time.Duration;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.SwiftBasket.cart_service.document.Cart;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CartCacheService {

    private static final Duration TTL = Duration.ofHours(24);

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    public Optional<Cart> get(Long userId, String sessionId) {
        try {
            String key = buildKey(userId, sessionId);
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) {
                log.info("Cache miss - key: {}", key);
                return Optional.empty();
            }
            Cart cart = objectMapper.readValue(json, Cart.class);
            log.info("Cache hit - key: {}, items: {}", key, cart.getItems().size());
            return Optional.of(cart);
        } catch (Exception e) {
            // cache is an optimization, never a dependency - degrade to Mongo
            log.warn("Cache read failed, degrading to Mongo - userId: {}, sessionId: {}",
                    userId, sessionId, e);
            return Optional.empty();
        }
    }

    public void put(Cart cart) {
        try {
            String key = buildKey(cart.getUserId(), cart.getSessionId());
            String json = objectMapper.writeValueAsString(cart);
            redisTemplate.opsForValue().set(key, json, TTL);
            log.info("Cart cached - key: {}, items: {}", key, cart.getItems().size());
        } catch (Exception e) {
            log.warn("Cache write failed, continuing without cache - cartId: {}",
                    cart.getId(), e);
        }
    }

    public void evict(Long userId, String sessionId) {
        try {
            String key = buildKey(userId, sessionId);
            redisTemplate.delete(key);
            log.info("Cache evicted - key: {}", key);
        } catch (Exception e) {
            log.warn("Cache evict failed - userId: {}, sessionId: {}", userId, sessionId, e);
        }
    }

    // Report: key format is cart:{userId} for logged-in users; guests use cart:session:{sessionId}
    private String buildKey(Long userId, String sessionId) {
        if (userId != null) {
            return "cart:" + userId;
        }
        return "cart:session:" + sessionId;
    }
}
