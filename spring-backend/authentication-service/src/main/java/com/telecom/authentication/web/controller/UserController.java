package com.telecom.authentication.web.controller;

import com.telecom.authentication.application.UserService;
import com.telecom.authentication.domain.model.Role;
import com.telecom.authentication.web.dto.CreateUserRequest;
import com.telecom.authentication.web.dto.UpdateUserRequest;
import com.telecom.authentication.web.dto.UserDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * User management endpoints. Protected — only ADMIN can access.
 * 
 * The API Gateway forwards X-Auth-Role header from JWT claims.
 * This controller checks the header to enforce admin-only access.
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    /**
     * List all users.
     */
    @GetMapping
    public ResponseEntity<List<UserDto>> getAllUsers(
            @RequestHeader(value = "X-Auth-Role", required = false) String role) {
        requireAdmin(role);
        return ResponseEntity.ok(userService.getAllUsers());
    }

    /**
     * Get user by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUser(
            @PathVariable Long id,
            @RequestHeader(value = "X-Auth-Role", required = false) String role) {
        requireAdmin(role);
        return ResponseEntity.ok(userService.getUserById(id));
    }

    /**
     * Get user by username.
     */
    @GetMapping("/username/{username}")
    public ResponseEntity<UserDto> getUserByUsername(
            @PathVariable String username,
            @RequestHeader(value = "X-Auth-Role", required = false) String role) {
        requireAdmin(role);
        return ResponseEntity.ok(userService.getUserByUsername(username));
    }

    /**
     * List users by role.
     */
    @GetMapping("/role/{roleName}")
    public ResponseEntity<List<UserDto>> getUsersByRole(
            @PathVariable String roleName,
            @RequestHeader(value = "X-Auth-Role", required = false) String role) {
        requireAdmin(role);
        Role targetRole = Role.valueOf(roleName.toUpperCase());
        return ResponseEntity.ok(userService.getUsersByRole(targetRole));
    }

    /**
     * List users by boutique.
     */
    @GetMapping("/boutique/{boutiqueId}")
    public ResponseEntity<List<UserDto>> getUsersByBoutique(
            @PathVariable Long boutiqueId,
            @RequestHeader(value = "X-Auth-Role", required = false) String role) {
        requireAdmin(role);
        return ResponseEntity.ok(userService.getUsersByBoutique(boutiqueId));
    }

    /**
     * Create a new user (RESPONSABLE_BOUTIQUE or AGENT_COMMERCIAL).
     */
    @PostMapping
    public ResponseEntity<UserDto> createUser(
            @Valid @RequestBody CreateUserRequest request,
            @RequestHeader(value = "X-Auth-Role", required = false) String role) {
        requireAdmin(role);
        UserDto created = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Update an existing user.
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserDto> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request,
            @RequestHeader(value = "X-Auth-Role", required = false) String role) {
        requireAdmin(role);
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    /**
     * Disable a user account.
     */
    @PostMapping("/{id}/disable")
    public ResponseEntity<UserDto> disableUser(
            @PathVariable Long id,
            @RequestHeader(value = "X-Auth-Role", required = false) String role) {
        requireAdmin(role);
        return ResponseEntity.ok(userService.disableUser(id));
    }

    /**
     * Re-enable a disabled user account.
     */
    @PostMapping("/{id}/enable")
    public ResponseEntity<UserDto> enableUser(
            @PathVariable Long id,
            @RequestHeader(value = "X-Auth-Role", required = false) String role) {
        requireAdmin(role);
        return ResponseEntity.ok(userService.enableUser(id));
    }

    /**
     * Check that the caller has ADMIN role (forwarded by API Gateway).
     */
    private void requireAdmin(String role) {
        if (role == null || !Role.ADMIN.name().equals(role)) {
            throw new SecurityException("Access denied. ADMIN role required.");
        }
    }
}
