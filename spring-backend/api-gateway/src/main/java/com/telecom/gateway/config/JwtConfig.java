package com.telecom.gateway.config;

import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * JWT Configuration for the API Gateway.
 * 
 * Holds the shared signing key used to validate tokens issued by the customer-service.
 * The secret MUST match the one configured in customer-service's application.yml.
 */
@Configuration
@Getter
@Slf4j
public class JwtConfig {

    private final SecretKey signingKey;

    public JwtConfig(@Value("${jwt.secret}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        log.info("JWT configuration initialized for API Gateway");
    }
}
