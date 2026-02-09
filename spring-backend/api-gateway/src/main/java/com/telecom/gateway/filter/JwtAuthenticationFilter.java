package com.telecom.gateway.filter;

import com.telecom.gateway.security.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.List;


@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;

    private static final List<String> PUBLIC_PATHS = List.of(
        "/api/auth/login",
        "/api/auth/register",
        "/api/auth/checkuser",
        "/api/auth/refresh",
        "/actuator",
        "/swagger-ui",
        "/v3/api-docs",
        "/webjars"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }
        
        String authHeader = exchange.getRequest().getHeaders()
            .getFirst(HttpHeaders.AUTHORIZATION);
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header for path: {}", path);
            return onError(exchange, HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
        }
        
        String token = authHeader.substring(7);
        
        if (token.isBlank()) {
            return onError(exchange, HttpStatus.UNAUTHORIZED, "Empty bearer token");
        }
        
        Claims claims;
        try {
            claims = jwtUtil.validateToken(token);
        } catch (JwtUtil.JwtValidationException ex) {
            log.warn("JWT validation failed for path {}: {}", path, ex.getMessage());
            return onError(exchange, HttpStatus.UNAUTHORIZED, ex.getMessage());
        }
        
        // Reject refresh tokens for API access
        if (jwtUtil.isRefreshToken(claims)) {
            log.warn("Refresh token used for API access on path: {}", path);
            return onError(exchange, HttpStatus.UNAUTHORIZED, "Refresh tokens cannot be used for API access");
        }
        
        // Extract user claims and forward to downstream services
        String subject = jwtUtil.getSubject(claims);
        String role = jwtUtil.getRole(claims);
        String email = jwtUtil.getEmail(claims);
        String customerRef = jwtUtil.getCustomerRef(claims);
        
        log.debug("Authenticated request from user: {} [role={}] to path: {}", subject, role, path);
        
        var mutatedRequest = exchange.getRequest().mutate()
                .header("X-Auth-User", subject != null ? subject : "")
                .header("X-Auth-Role", role != null ? role : "")
                .header("X-Auth-Email", email != null ? email : "")
                .header("X-Auth-CustomerRef", customerRef != null ? customerRef : "")
                .build();
        
        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }
    
    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

   
    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus status, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        
        String body = """
                {"error": "%s", "status": %d, "path": "%s"}
                """.formatted(message, status.value(), exchange.getRequest().getPath().value());
        
        DataBuffer buffer = exchange.getResponse()
                .bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
