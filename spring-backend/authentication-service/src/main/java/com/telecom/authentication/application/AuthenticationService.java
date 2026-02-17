package com.telecom.authentication.application;

import com.telecom.authentication.domain.model.*;
import com.telecom.authentication.domain.repository.AuthEventRepository;
import com.telecom.authentication.domain.repository.RefreshTokenRepository;
import com.telecom.authentication.domain.repository.UserRepository;
import com.telecom.authentication.security.JwtService;
import com.telecom.authentication.web.dto.LoginRequest;
import com.telecom.authentication.web.dto.LoginResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;

/**
 * Core authentication service handling login, logout, and token refresh.
 * 
 * Enforces single-session: on login, all previous refresh tokens are revoked.
 * Admin credentials are hardcoded as a bootstrap mechanism.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthEventRepository authEventRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username}")
    private String adminUsername;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Value("${app.security.max-failed-attempts}")
    private int maxFailedAttempts;

    /**
     * Authenticate a user by username and password.
     * 
     * Flow:
     * 1. Check hardcoded admin credentials (bootstrap)
     * 2. Check database users with BCrypt validation
     * 3. Enforce single session (revoke all previous tokens)
     * 4. Generate access + refresh tokens
     * 5. Store refresh token hash in database
     */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        String username = request.username().trim();
        String password = request.password();

        log.info("Login attempt for username: {}, adminUsername value: {}, adminPassword value: {}", username, adminUsername, adminPassword != null ? "SET" : "NULL");

        if (adminUsername != null && adminUsername.equals(username) && adminPassword != null && adminPassword.equals(password)) {
            log.info("Hardcoded admin credentials matched!");
            var dbAdmin = userRepository.findByUsername(username);
            if (dbAdmin.isEmpty()) {
                log.info("Admin login via hardcoded credentials: {}", username);
                LoginResponse response = generateLoginResponse(username, Role.ADMIN, null);
                logAuthEvent(username, AuthEvent.AuthEventType.LOGIN, true, "Hardcoded admin login");
                return response;
            }
            log.info("Admin exists in DB, falling through to DB validation");
        } else {
            log.info("Hardcoded admin check failed. adminUsername.equals(username)={}, adminPassword.equals(password)={}", 
                adminUsername != null ? adminUsername.equals(username) : false,
                adminPassword != null ? adminPassword.equals(password) : false);
        }

        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            logAuthEvent(username, AuthEvent.AuthEventType.FAILED_LOGIN, false, "User not found");
            return LoginResponse.failure("Invalid credentials");
        }

        if (user.getStatus() == UserStatus.DISABLED) {
            logAuthEvent(username, AuthEvent.AuthEventType.FAILED_LOGIN, false, "Account disabled");
            return LoginResponse.failure("Account is disabled. Contact administrator.");
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            logAuthEvent(username, AuthEvent.AuthEventType.FAILED_LOGIN, false, "Invalid password");
            return LoginResponse.failure("Invalid credentials");
        }

        user.recordSuccessfulLogin();
        userRepository.save(user);

        refreshTokenRepository.revokeAllByUser(user);

        LoginResponse response = generateLoginResponse(
                user.getUsername(),
                user.getRole(),
                user.getBoutiqueId()
        );

        storeRefreshToken(response.refreshToken(), user);

        logAuthEvent(username, AuthEvent.AuthEventType.LOGIN, true, "Login successful");
        log.info("User logged in: {} [role={}]", username, user.getRole());
        return response;
    }

    /**
     * Refresh an access token using a valid refresh token.
     * Rotates the refresh token (old one is revoked, new one issued).
     */
    @Transactional
    public LoginResponse refresh(String refreshTokenStr) {
        Claims claims;
        try {
            claims = jwtService.parseToken(refreshTokenStr);
        } catch (JwtException e) {
            log.warn("Refresh token validation failed: {}", e.getMessage());
            return LoginResponse.failure("Invalid or expired refresh token");
        }

        if (!jwtService.isRefreshToken(claims)) {
            return LoginResponse.failure("Provided token is not a refresh token");
        }

        // Verify token is in DB and not revoked
        String tokenHash = hashToken(refreshTokenStr);
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash).orElse(null);
        if (storedToken == null || !storedToken.isValid()) {
            log.warn("Refresh token not found or revoked for user: {}", claims.getSubject());
            return LoginResponse.failure("Refresh token has been revoked");
        }

        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        User user = storedToken.getUser();
        if (!user.canLogin()) {
            return LoginResponse.failure("Account is not active");
        }

        LoginResponse response = generateLoginResponse(
                user.getUsername(),
                user.getRole(),
                user.getBoutiqueId()
        );

        storeRefreshToken(response.refreshToken(), user);

        logAuthEvent(user.getUsername(), AuthEvent.AuthEventType.REFRESH, true, "Token refreshed");
        return response;
    }

    /**
     * Logout: revoke all refresh tokens for the user.
     */
    @Transactional
    public void logout(String refreshTokenStr) {
        try {
            Claims claims = jwtService.parseToken(refreshTokenStr);
            String username = claims.getSubject();

            userRepository.findByUsername(username).ifPresent(user -> {
                refreshTokenRepository.revokeAllByUser(user);
                logAuthEvent(username, AuthEvent.AuthEventType.LOGOUT, true, "Logged out");
                log.info("User logged out: {}", username);
            });
        } catch (JwtException e) {
            log.debug("Logout with invalid token: {}", e.getMessage());
        }
    }

    /**
     * Validate an access token and return its claims.
     */
    public Map<String, Object> validateToken(String token) {
        try {
            Claims claims = jwtService.parseToken(token);
            return Map.of(
                    "valid", true,
                    "username", claims.getSubject(),
                    "role", jwtService.extractRole(claims),
                    "expiresAt", claims.getExpiration().toInstant().toString()
            );
        } catch (JwtException e) {
            return Map.of("valid", false);
        }
    }

    private LoginResponse generateLoginResponse(String username, Role role, Long boutiqueId) {
        String accessToken = jwtService.generateAccessToken(
                username,
                role.name(),
                boutiqueId
        );
        String refreshToken = jwtService.generateRefreshToken(username, role.name());

        // For hardcoded admin without a DB record
        String firstName = "Admin";
        String lastName = "";

        var userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            firstName = userOpt.get().getFirstName();
            lastName = userOpt.get().getLastName();
        }

        return LoginResponse.success(
                accessToken,
                refreshToken,
                role.name(),
                username,
                firstName,
                lastName,
                boutiqueId
        );
    }

    private void storeRefreshToken(String rawToken, User user) {
        RefreshToken token = RefreshToken.builder()
                .tokenHash(hashToken(rawToken))
                .user(user)
                .revoked(false)
                .expiresAt(LocalDateTime.now().plusSeconds(jwtService.getRefreshTokenExpirationMs() / 1000))
                .build();
        refreshTokenRepository.save(token);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private void logAuthEvent(String username, AuthEvent.AuthEventType eventType, boolean success, String details) {
        authEventRepository.save(AuthEvent.builder()
                .username(username)
                .eventType(eventType)
                .success(success)
                .details(details)
                .build());
    }
}
