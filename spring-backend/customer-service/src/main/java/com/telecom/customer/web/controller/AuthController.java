package com.telecom.customer.web.controller;

import com.telecom.customer.application.CustomerService;
import com.telecom.customer.security.JwtService;
import com.telecom.customer.web.dto.ClientDto;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.HashMap;

/**
 * AUTH CONTROLLER - JWT-based authentication.
 * 
 * Issues signed JWT access tokens and refresh tokens.
 * Access tokens (24h) carry user claims for downstream services.
 * Refresh tokens (7d) can be exchanged for new access tokens.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    
    private final CustomerService customerService;
    private final JwtService jwtService;
    
    // Demo admin credentials
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin123";
    
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("userName");
        String password = credentials.get("password");
        
        Map<String, Object> response = new HashMap<>();
        
        // Check admin login
        if (ADMIN_USERNAME.equals(username) && ADMIN_PASSWORD.equals(password)) {
            String accessToken = jwtService.generateAccessToken(username, Map.of(
                    "role", "admin",
                    "email", username
            ));
            String refreshToken = jwtService.generateRefreshToken(username, "admin");
            
            response.put("login", true);
            response.put("userName", true);
            response.put("password", true);
            response.put("role", "admin");
            response.put("token", accessToken);
            response.put("refreshToken", refreshToken);
            log.info("Admin login successful for user: {}", username);
            return ResponseEntity.ok(response);
        }
        
        // Check customer login by email
        try {
            ClientDto customer = customerService.getCustomerByEmail(username);
            // For demo, we accept any password for customers
            if (customer != null) {
                String accessToken = jwtService.generateAccessToken(username, Map.of(
                        "role", "customer",
                        "email", username,
                        "customerRef", customer.customerRef(),
                        "customerName", customer.prenom() + " " + customer.nom()
                ));
                String refreshToken = jwtService.generateRefreshToken(username, "customer");
                
                response.put("login", true);
                response.put("userName", true);
                response.put("password", true);
                response.put("role", "customer");
                response.put("customerId", customer.customerRef());
                response.put("customerName", customer.prenom() + " " + customer.nom());
                response.put("token", accessToken);
                response.put("refreshToken", refreshToken);
                log.info("Customer login successful for: {}", username);
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            log.debug("Customer lookup failed for: {}", username);
        }
        
        // Login failed
        response.put("login", false);
        response.put("userName", false);
        response.put("password", false);
        response.put("message", "Invalid credentials");
        log.warn("Login failed for user: {}", username);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/checkuser")
    public ResponseEntity<Map<String, Object>> checkUser(@RequestBody Map<String, String> credentials) {
        // Alias for login endpoint for backward compatibility
        return login(credentials);
    }
    
    /**
     * Validate an existing JWT token.
     * Returns token validity status and extracted claims.
     */
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Map<String, Object> response = new HashMap<>();
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                Claims claims = jwtService.parseToken(token);
                response.put("valid", true);
                response.put("role", claims.get("role", String.class));
                response.put("subject", claims.getSubject());
                response.put("expiresAt", claims.getExpiration().toInstant().toString());
                return ResponseEntity.ok(response);
            } catch (JwtException e) {
                log.debug("Token validation failed: {}", e.getMessage());
            }
        }
        
        response.put("valid", false);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Refresh an access token using a valid refresh token.
     * 
     * The client sends the refresh token in the Authorization header.
     * If the refresh token is valid and not expired, a new access token
     * (and optionally a rotated refresh token) is issued.
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshToken(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Map<String, Object> response = new HashMap<>();
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.put("error", "Missing refresh token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        
        String refreshToken = authHeader.substring(7);
        
        try {
            Claims claims = jwtService.parseToken(refreshToken);
            
            // Ensure this is actually a refresh token
            if (!jwtService.isRefreshToken(claims)) {
                response.put("error", "Provided token is not a refresh token");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            String subject = claims.getSubject();
            String role = jwtService.extractRole(claims);
            
            // Build new access token with fresh claims
            Map<String, Object> accessClaims = new HashMap<>();
            accessClaims.put("role", role);
            accessClaims.put("email", subject);
            
            // For customer role, re-fetch customer details for fresh claims
            if ("customer".equals(role)) {
                try {
                    ClientDto customer = customerService.getCustomerByEmail(subject);
                    if (customer != null) {
                        accessClaims.put("customerRef", customer.customerRef());
                        accessClaims.put("customerName", customer.prenom() + " " + customer.nom());
                    }
                } catch (Exception e) {
                    log.warn("Could not refresh customer details for: {}", subject);
                }
            }
            
            String newAccessToken = jwtService.generateAccessToken(subject, accessClaims);
            // Rotate refresh token for enhanced security
            String newRefreshToken = jwtService.generateRefreshToken(subject, role);
            
            response.put("token", newAccessToken);
            response.put("refreshToken", newRefreshToken);
            response.put("role", role);
            log.info("Token refreshed for user: {}", subject);
            return ResponseEntity.ok(response);
            
        } catch (JwtException e) {
            log.warn("Refresh token validation failed: {}", e.getMessage());
            response.put("error", "Invalid or expired refresh token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }
}
