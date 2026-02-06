package com.telecom.gateway.security;

import com.telecom.gateway.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * JWT Utility for the API Gateway.
 * 
 * Provides token parsing, signature verification, expiration checking,
 * and claims extraction. Used by JwtAuthenticationFilter to validate
 * incoming requests.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtUtil {

    private final JwtConfig jwtConfig;

    /**
     * Validate and parse a JWT token.
     * 
     * This method performs:
     * 1. Signature verification using HMAC-SHA256
     * 2. Expiration check (built into jjwt parser)
     * 3. Structural validation (well-formed JWT)
     *
     * @param token the raw JWT string (without "Bearer " prefix)
     * @return parsed Claims if valid
     * @throws JwtValidationException with specific reason if validation fails
     */
    public Claims validateToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(jwtConfig.getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException ex) {
            log.warn("JWT token expired for subject: {}", ex.getClaims().getSubject());
            throw new JwtValidationException("Token has expired", ex);
        } catch (io.jsonwebtoken.security.SecurityException ex) {
            log.error("JWT signature verification failed");
            throw new JwtValidationException("Invalid token signature", ex);
        } catch (io.jsonwebtoken.MalformedJwtException ex) {
            log.error("Malformed JWT token");
            throw new JwtValidationException("Malformed token", ex);
        } catch (JwtException ex) {
            log.error("JWT validation error: {}", ex.getMessage());
            throw new JwtValidationException("Token validation failed", ex);
        }
    }

    /**
     * Extract the subject (username/email) from claims.
     */
    public String getSubject(Claims claims) {
        return claims.getSubject();
    }

    /**
     * Extract the role from claims.
     */
    public String getRole(Claims claims) {
        return claims.get("role", String.class);
    }

    /**
     * Extract the customer reference from claims (null for admin users).
     */
    public String getCustomerRef(Claims claims) {
        return claims.get("customerRef", String.class);
    }

    /**
     * Extract the email from claims.
     */
    public String getEmail(Claims claims) {
        return claims.get("email", String.class);
    }

    /**
     * Check if the token is a refresh token.
     */
    public boolean isRefreshToken(Claims claims) {
        return "refresh".equals(claims.get("type", String.class));
    }

    /**
     * Custom exception for JWT validation failures with descriptive reasons.
     */
    public static class JwtValidationException extends RuntimeException {
        public JwtValidationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
