package com.telecom.authentication.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Audit log for authentication events (login, logout, failures).
 */
@Entity
@Table(name = "auth_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100)
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private AuthEventType eventType;

    @Column(nullable = false)
    private boolean success;

    @Column(length = 500)
    private String details;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum AuthEventType {
        LOGIN,
        LOGOUT,
        REFRESH,
        FAILED_LOGIN,
        ACCOUNT_LOCKED,
        PASSWORD_CHANGED,
        USER_CREATED,
        USER_UPDATED,
        USER_DISABLED
    }
}
