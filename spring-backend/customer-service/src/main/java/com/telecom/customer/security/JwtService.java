package com.telecom.customer.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

/**
 * JWT Service - Generates and validates JSON Web Tokens.
 * 
 * Uses HMAC-SHA256 signing with a shared secret key.
 * Tokens contain standard claims (sub, iat, exp) plus custom claims
 * (role, email, customerRef) for downstream service authorization.
 */
@Service
@Slf4j
public class JwtService {

    private final SecretKey signingKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-expiration:604800000}") long refreshTokenExpiration) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    /**
     * Generate an access token with user claims.
     *
     * @param subject     the token subject (username or email)
     * @param extraClaims additional claims to embed (role, customerRef, etc.)
     * @return signed JWT string
     */
    public String generateAccessToken(String subject, Map<String, Object> extraClaims) {
        return buildToken(subject, extraClaims, accessTokenExpiration);
    }

    /**
     * Generate a refresh token with minimal claims.
     * Refresh tokens have a longer expiration and carry only the subject + role.
     *
     * @param subject the token subject
     * @param role    the user role
     * @return signed JWT refresh token string
     */
    public String generateRefreshToken(String subject, String role) {
        return buildToken(subject, Map.of("role", role, "type", "refresh"), refreshTokenExpiration);
    }

    /**
     * Parse and validate a JWT token.
     *
     * @param token the JWT string
     * @return parsed Claims if valid
     * @throws io.jsonwebtoken.JwtException if token is invalid, expired, or tampered with
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Check whether a token is a refresh token.
     */
    public boolean isRefreshToken(Claims claims) {
        return "refresh".equals(claims.get("type", String.class));
    }

    /**
     * Extract the role claim from parsed claims.
     */
    public String extractRole(Claims claims) {
        return claims.get("role", String.class);
    }

    private String buildToken(String subject, Map<String, Object> extraClaims, long expirationMillis) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(expirationMillis);

        var builder = Jwts.builder()
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .issuer("telecom-billing-platform");

        extraClaims.forEach(builder::claim);

        return builder.signWith(signingKey).compact();
    }
}
