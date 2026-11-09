package com.SwiftBasket.notification_service.client;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.SwiftBasket.notification_service.exception.UserEmailResolutionException;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Claim-check email resolution: events carry userId; this client resolves the
 * user's CURRENT email at send time via user-service endpoint 1.8, with a
 * short-TTL cache so hot paths do not hammer user-service.
 */
@Component
@Slf4j
public class UserClient {

    private final RestClient restClient;
    private final long ttlMillis;

    // userId -> cached email with expiry timestamp
    private final Map<Long, CachedEmail> cache = new ConcurrentHashMap<>();

    private static class CachedEmail {
        private final String email;
        private final Instant cachedAt;

        private CachedEmail(String email, Instant cachedAt) {
            this.email = email;
            this.cachedAt = cachedAt;
        }

        private boolean isFresh(long ttlMillis) {
            return Instant.now().isBefore(cachedAt.plusMillis(ttlMillis));
        }
    }

    public UserClient(@Value("${user.service.url}") String baseUrl,
                      @Value("${user.cache.ttl-minutes:15}") long ttlMinutes) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.ttlMillis = ttlMinutes * 60_000;
        log.info("UserClient initialized - baseUrl: {}, email cache TTL: {} minutes", baseUrl, ttlMinutes);
    }

    // minimal projection of user-service's UserEmailResponse - only what notifications need
    @Data
    public static class UserEmailData {
        private Long userId;
        private String email;
    }

    public String getEmail(Long userId) {
        CachedEmail cached = cache.get(userId);
        if (cached != null && cached.isFresh(ttlMillis)) {
            log.debug("Email cache hit for userId {}", userId);
            return cached.email;
        }

        log.info("Resolving email for userId {} from user-service", userId);
        try {
            UserEmailData data = restClient.get()
                    .uri("/api/v1/users/{id}/email", userId)
                    .retrieve()
                    .body(UserEmailData.class);

            if (data == null || data.getEmail() == null) {
                log.warn("User email response was empty for userId {}", userId);
                throw new UserEmailResolutionException("Could not resolve email for user " + userId);
            }

            cache.put(userId, new CachedEmail(data.getEmail(), Instant.now()));
            return data.getEmail();

        } catch (RestClientResponseException e) {
            log.error("User service rejected email lookup for userId {} - status: {}", userId, e.getStatusCode());
            throw new UserEmailResolutionException("Could not resolve email for user " + userId);

        } catch (UserEmailResolutionException e) {
            throw e;

        } catch (Exception e) {
            log.error("User service unavailable while resolving email for userId {}: {}", userId, e.getMessage());
            throw new UserEmailResolutionException("Could not resolve email for user " + userId);
        }
    }
}
