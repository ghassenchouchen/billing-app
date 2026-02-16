package com.telecom.authentication.web.controller;

import com.telecom.authentication.application.AuthenticationService;
import com.telecom.authentication.web.dto.LoginRequest;
import com.telecom.authentication.web.dto.LoginResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Authentication endpoints: login, logout, refresh, validate.
 * All endpoints are public (no JWT required).
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationService authenticationService;

    /**
     * Login with username and password.
     * Returns access token + refresh token on success.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authenticationService.login(request);
        if (response.success()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * Backward-compatible alias for /login.
     */
    @PostMapping("/checkuser")
    public ResponseEntity<LoginResponse> checkUser(@Valid @RequestBody LoginRequest request) {
        return login(request);
    }

    /**
     * Refresh an access token using a valid refresh token.
     * The old refresh token is revoked and a new one is issued (rotation).
     */
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(LoginResponse.failure("Missing refresh token"));
        }

        String refreshToken = authHeader.substring(7);
        LoginResponse response = authenticationService.refresh(refreshToken);

        if (response.success()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * Logout: revoke all refresh tokens for the user.
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            authenticationService.logout(authHeader.substring(7));
        }
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    /**
     * Validate a JWT token and return its claims.
     */
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validate(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.ok(Map.of("valid", false));
        }

        Map<String, Object> result = authenticationService.validateToken(authHeader.substring(7));
        return ResponseEntity.ok(result);
    }
}
