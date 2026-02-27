package com.telecom.authentication.application;

import com.telecom.authentication.domain.model.*;
import com.telecom.authentication.domain.repository.AuthEventRepository;
import com.telecom.authentication.domain.repository.RefreshTokenRepository;
import com.telecom.authentication.domain.repository.UserRepository;
import com.telecom.authentication.web.dto.CreateUserRequest;
import com.telecom.authentication.web.dto.UpdateUserRequest;
import com.telecom.authentication.web.dto.UserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * User management service. Only ADMIN can create/update/disable users.
 * 
 * Enforces:
 * - Username uniqueness
 * - RESPONSABLE_BOUTIQUE and AGENT_COMMERCIAL must have a boutiqueId
 * - ADMIN cannot have a boutiqueId
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthEventRepository authEventRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserDto getUserById(Long id) {
        return userRepository.findById(id)
                .map(UserDto::from)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
    }

    @Transactional(readOnly = true)
    public UserDto getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(UserDto::from)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }

    @Transactional(readOnly = true)
    public List<UserDto> getUsersByRole(Role role) {
        return userRepository.findByRole(role).stream()
                .map(UserDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserDto> getUsersByBoutique(Long boutiqueId) {
        return userRepository.findByBoutiqueId(boutiqueId).stream()
                .map(UserDto::from)
                .toList();
    }

    /**
     * Create a new user. Only ADMIN can call this.
     * 
     * Rules:
     * - Username must be unique (format: lastname.firstname)
     * - RESPONSABLE_BOUTIQUE / AGENT_COMMERCIAL must have boutiqueId
     * - ADMIN role cannot be assigned (there is only one hardcoded admin)
     */
    @Transactional
    public UserDto createUser(CreateUserRequest request) {
        // Validate role - cannot create another ADMIN
        if (request.role() == Role.ADMIN) {
            throw new IllegalArgumentException("Cannot create additional ADMIN users");
        }

        // Validate username uniqueness
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Username already exists: " + request.username());
        }

        // Validate boutique assignment
        if ((request.role() == Role.RESPONSABLE_BOUTIQUE || request.role() == Role.AGENT_COMMERCIAL)
                && request.boutiqueId() == null) {
            throw new IllegalArgumentException(request.role() + " must be assigned to a boutique");
        }

        User user = User.builder()
                .username(request.username().toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(request.password()))
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .role(request.role())
                .status(UserStatus.ACTIVE)
                .boutiqueId(request.boutiqueId())
                .build();

        user = userRepository.save(user);

        logAuthEvent(request.username(), AuthEvent.AuthEventType.USER_CREATED,
                "Created with role " + request.role());

        log.info("User created: {} [role={}, boutiqueId={}]",
                user.getUsername(), user.getRole(), user.getBoutiqueId());

        return UserDto.from(user);
    }

    /**
     * Update an existing user. Only ADMIN can call this.
     */
    @Transactional
    public UserDto updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));

        if (request.firstName() != null) {
            user.setFirstName(request.firstName().trim());
        }
        if (request.lastName() != null) {
            user.setLastName(request.lastName().trim());
        }
        if (request.password() != null) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
            logAuthEvent(user.getUsername(), AuthEvent.AuthEventType.PASSWORD_CHANGED, "Password changed by admin");
        }
        if (request.role() != null) {
            if (request.role() == Role.ADMIN) {
                throw new IllegalArgumentException("Cannot assign ADMIN role");
            }
            user.setRole(request.role());
        }
        if (request.boutiqueId() != null) {
            user.setBoutiqueId(request.boutiqueId());
        }

        user = userRepository.save(user);

        logAuthEvent(user.getUsername(), AuthEvent.AuthEventType.USER_UPDATED, "User updated by admin");
        log.info("User updated: {}", user.getUsername());

        return UserDto.from(user);
    }

    /**
     * Disable a user account. Revokes all tokens.
     */
    @Transactional
    public UserDto disableUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));

        user.setStatus(UserStatus.DISABLED);
        userRepository.save(user);

        // Revoke all tokens
        refreshTokenRepository.revokeAllByUser(user);

        logAuthEvent(user.getUsername(), AuthEvent.AuthEventType.USER_DISABLED, "Account disabled by admin");
        log.info("User disabled: {}", user.getUsername());

        return UserDto.from(user);
    }

    /**
     * Re-enable a disabled user account.
     */
    @Transactional
    public UserDto enableUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));

        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        logAuthEvent(user.getUsername(), AuthEvent.AuthEventType.USER_UPDATED, "Account re-enabled by admin");
        log.info("User enabled: {}", user.getUsername());

        return UserDto.from(user);
    }

    /**
     * Delete a user account permanently. Revokes all tokens and deletes the user.
     */
    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));

        String username = user.getUsername();

        // Delete all refresh tokens for this user first (to avoid foreign key constraint violation)
        refreshTokenRepository.deleteByUser(user);

        // Delete the user
        userRepository.deleteById(id);

        logAuthEvent(username, AuthEvent.AuthEventType.USER_DISABLED, "Account permanently deleted by admin");
        log.info("User deleted: {}", username);
    }

    private void logAuthEvent(String username, AuthEvent.AuthEventType eventType, String details) {
        authEventRepository.save(AuthEvent.builder()
                .username(username)
                .eventType(eventType)
                .success(true)
                .details(details)
                .build());
    }
}
