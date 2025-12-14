package com.SwiftBasket.user_service.util;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
            return Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            log.error("Error extracting claims from token: {}", e.getMessage());
            return null;
        }
    }

    public String generateToken(Long userId, String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSecretKey(), SIG.HS512)
                .compact();
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = extractClaims(token);
        try {
            return claims != null ? Long.valueOf(claims.getSubject()) : null;
        } catch (NumberFormatException e) {
            log.error("Error parsing user ID: {}", e.getMessage());
            return null;
        }
    }

    public String getEmailFromToken(String token) {
        Claims claims = extractClaims(token);
        return claims != null ? claims.get("email", String.class) : null;
    }

    public boolean validateToken(String token) {
        return extractClaims(token) != null;
    }

    public boolean isTokenExpired(String token) {
        Claims claims = extractClaims(token);
        return claims == null || claims.getExpiration().before(new Date());
    }

    public Long getExpirationTimeInMs() {
        return jwtExpirationInMs;
    }
}
