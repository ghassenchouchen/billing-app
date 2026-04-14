package com.telecom.authentication.security;

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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * JWT Service for token generation and validation.
 */

@Service
@Slf4j
public class JwtService {

    private final SecretKey signingKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-expiration}") long refreshTokenExpiration) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    public String generateAccessToken(String username, String role, Long boutiqueId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        if (boutiqueId != null) {
            claims.put("boutiqueId", boutiqueId);
        }
        return buildToken(username, claims, accessTokenExpiration);
    }

    // keep the uesr logged in
    public String generateRefreshToken(String username, String role) {
        return buildToken(username, Map.of("role", role, "type", "refresh"), refreshTokenExpiration);
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isRefreshToken(Claims claims) {
        return "refresh".equals(claims.get("type", String.class));
    }

    public String extractRole(Claims claims) {
        return claims.get("role", String.class);
    }

    public String extractSubject(Claims claims) {
        return claims.getSubject();
    }

    public long getRefreshTokenExpirationMs() {
        return refreshTokenExpiration;
    }

    private String buildToken(String subject, Map<String, Object> extraClaims, long expirationMillis) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(expirationMillis);

        var builder = Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .issuer("telecom-billing-platform");

        extraClaims.forEach(builder::claim);

        return builder.signWith(signingKey).compact();
    }
}
