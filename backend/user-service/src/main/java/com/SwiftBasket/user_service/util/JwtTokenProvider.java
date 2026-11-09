package com.SwiftBasket.user_service.util;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.SwiftBasket.user_service.entity.Role;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Jwts.SIG;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private Long jwtExpirationInMs;

    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    private Claims extractClaims(String token) {
        try {
            log.debug("Attempting to extract claims from token");
            return Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (io.jsonwebtoken.security.SignatureException e) {
            log.warn("Invalid token signature: {}", e.getMessage());
            return null;
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            log.warn("Token has expired: {}", e.getMessage());
            return null;
        } catch (io.jsonwebtoken.MalformedJwtException e) {
            log.warn("Malformed JWT token: {}", e.getMessage());
            return null;
        } catch (JwtException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return null;
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty: {}", e.getMessage());
            return null;
        }
    }

    public String generateToken(Long userId, String email, Role role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);
        log.debug("Generating token for user ID: {} with expiration: {}", userId, jwtExpirationInMs + "ms");

        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("role", role.name())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSecretKey(), SIG.HS512)
                .compact();
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = extractClaims(token);
        if (claims == null) {
            log.warn("Cannot extract user ID from token - claims extraction failed");
            return null;
        }
        try {
            String subject = claims.getSubject();
            if (subject == null || subject.isEmpty()) {
                log.warn("Token subject is empty");
                return null;
            }
            return Long.valueOf(subject);
        } catch (NumberFormatException e) {
            log.error("Error parsing user ID from token subject: {}", e.getMessage());
            return null;
        }
    }

    public String getEmailFromToken(String token) {
        Claims claims = extractClaims(token);
        if (claims == null) {
            log.warn("Cannot extract email from token - claims extraction failed");
            return null;
        }
        return claims.get("email", String.class);
    }

    public String getRoleFromToken(String token) {
        Claims claims = extractClaims(token);
        if (claims == null) {
            log.warn("Cannot extract role from token - claims extraction failed");
            return null;
        }
        return claims.get("role", String.class);
    }

    public boolean validateToken(String token) {
        if (token == null || token.isEmpty()) {
            log.warn("Token is null or empty");
            return false;
        }
        Claims claims = extractClaims(token);
        if (claims == null) {
            log.debug("Token validation failed - could not extract claims");
            return false;
        }
        log.debug("Token validation successful");
        return true;
    }

    public boolean isTokenExpired(String token) {
        Claims claims = extractClaims(token);
        if (claims == null) {
            log.warn("Cannot check token expiration - claims extraction failed");
            return true;  // Treat as expired if we can't validate
        }
        Date expirationDate = claims.getExpiration();
        boolean isExpired = expirationDate != null && expirationDate.before(new Date());
        if (isExpired) {
            log.warn("Token is expired. Expiration date: {}", expirationDate);
        }
        return isExpired;
    }

    public Long getExpirationTimeInMs() {
        return jwtExpirationInMs;
    }
}
