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
  User management endpoints. Protected - only ADMIN can access.
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    
    @GetMapping
    public ResponseEntity<List<UserDto>> getAllUsers(
            @RequestHeader(value = "X-Auth-Role", required = false) String role) {
        requireAdmin(role);
        return ResponseEntity.ok(userService.getAllUsers());
    }

    
    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUser(
            @PathVariable Long id,
            @RequestHeader(value = "X-Auth-Role", required = false) String role) {
        requireAdmin(role);
        return ResponseEntity.ok(userService.getUserById(id));
    }

   
    @GetMapping("/username/{username}")
    public ResponseEntity<UserDto> getUserByUsername(
            @PathVariable String username,
            @RequestHeader(value = "X-Auth-Role", required = false) String role) {
        requireAdmin(role);
        return ResponseEntity.ok(userService.getUserByUsername(username));
    }

    
    @GetMapping("/role/{roleName}")
    public ResponseEntity<List<UserDto>> getUsersByRole(
            @PathVariable String roleName,
            @RequestHeader(value = "X-Auth-Role", required = false) String role) {
        requireAdmin(role);
        Role targetRole = Role.valueOf(roleName.toUpperCase());
        return ResponseEntity.ok(userService.getUsersByRole(targetRole));
    }

  
    @GetMapping("/boutique/{boutiqueId}")
    public ResponseEntity<List<UserDto>> getUsersByBoutique(
            @PathVariable Long boutiqueId,
            @RequestHeader(value = "X-Auth-Role", required = false) String role) {
        requireAdmin(role);
        return ResponseEntity.ok(userService.getUsersByBoutique(boutiqueId));
    }

    /*
      Create a new user (RESPONSABLE_BOUTIQUE ou AGENT_COMMERCIAL).
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

 
    @PostMapping("/{id}/disable")
    public ResponseEntity<UserDto> disableUser(
            @PathVariable Long id,
            @RequestHeader(value = "X-Auth-Role", required = false) String role) {
        requireAdmin(role);
        return ResponseEntity.ok(userService.disableUser(id));
    }

    @PostMapping("/{id}/enable")
    public ResponseEntity<UserDto> enableUser(
            @PathVariable Long id,
            @RequestHeader(value = "X-Auth-Role", required = false) String role) {
        requireAdmin(role);
        return ResponseEntity.ok(userService.enableUser(id));
    }

   
    private void requireAdmin(String role) {
        if (role == null || !Role.ADMIN.name().equals(role)) {
            throw new SecurityException("Access denied. ADMIN role required.");
        }
    }
}
