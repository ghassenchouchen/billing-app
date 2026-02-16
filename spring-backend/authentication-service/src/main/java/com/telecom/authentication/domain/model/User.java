package com.telecom.authentication.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * User entity for authentication and authorization.
 * 
 * Users are identified by username (format: lastname.firstname).
 * Each user has exactly one role in the hierarchy:
 * ADMIN > RESPONSABLE_BOUTIQUE > AGENT_COMMERCIAL
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

  
    @Column(name = "boutique_id")
    private Long boutiqueId;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = UserStatus.ACTIVE;
        }
        if (failedLoginAttempts == null) {
            failedLoginAttempts = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Check if the user can log in (active account).
     */
    public boolean canLogin() {
        return status == UserStatus.ACTIVE;
    }

    /**
     * Record a successful login: update timestamp.
     */
    public void recordSuccessfulLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }
}
