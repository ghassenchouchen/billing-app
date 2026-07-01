package com.telecom.authentication.web.controller;

import com.telecom.authentication.application.AuthenticationService;
import com.telecom.authentication.application.UserService;
import com.telecom.authentication.domain.repository.UserRepository;
import com.telecom.authentication.web.dto.LoginRequest;
import com.telecom.authentication.web.dto.LoginResponse;
import com.telecom.authentication.web.dto.SetPasswordRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Authentication endpoints: login, logout, refresh, validate.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationService authenticationService;
    private final UserService userService;
    private final UserRepository userRepository;

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

    
    @PostMapping("/checkuser")
    public ResponseEntity<LoginResponse> checkUser(@Valid @RequestBody LoginRequest request) {
        return login(request);
    }

    /**
     * Refresh an access token using a valid refresh token.
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

    
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            authenticationService.logout(authHeader.substring(7));
        }
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

  
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validate(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.ok(Map.of("valid", false));
        }

        Map<String, Object> result = authenticationService.validateToken(authHeader.substring(7));
        return ResponseEntity.ok(result);
    }

    /**
     * Check if a set-password token is valid.
     */
    @GetMapping("/verify-token")
    public ResponseEntity<Map<String, Object>> verifyToken(@RequestParam String token) {
        var userOpt = userRepository.findBySetPasswordToken(token);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("valid", false, "message", "Token invalide ou inexistant."));
        }
        var user = userOpt.get();
        if (user.getSetPasswordTokenExpiresAt() != null && 
                user.getSetPasswordTokenExpiresAt().isBefore(java.time.LocalDateTime.now())) {
            return ResponseEntity.badRequest().body(Map.of("valid", false, "message", "Le lien a expiré. Veuillez contacter l'administrateur."));
        }
        return ResponseEntity.ok(Map.of("valid", true, "username", user.getUsername(), "email", user.getEmail()));
    }

    /**
     * Set a user's password using the set-password token.
     */
    @PostMapping("/set-password")
    public ResponseEntity<Map<String, String>> setPassword(@Valid @RequestBody SetPasswordRequest request) {
        try {
            userService.setPassword(request.token(), request.password());
            return ResponseEntity.ok(Map.of("message", "Mot de passe défini avec succès ! Vous pouvez maintenant vous connecter."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
