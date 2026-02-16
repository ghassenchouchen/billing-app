-- =====================================================
-- V1: Authentication service schema
-- =====================================================

-- Users table
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    role ENUM('ADMIN', 'RESPONSABLE_BOUTIQUE', 'AGENT_COMMERCIAL') NOT NULL,
    status ENUM('ACTIVE', 'DISABLED') NOT NULL DEFAULT 'ACTIVE',
    boutique_id BIGINT NULL,
    failed_login_attempts INT NOT NULL DEFAULT 0,
    last_login_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_users_role (role),
    INDEX idx_users_status (status),
    INDEX idx_users_boutique (boutique_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Application users with role-based access';

-- Refresh tokens table (single-session enforcement)
CREATE TABLE refresh_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_refresh_token_user (user_id),
    INDEX idx_refresh_token_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Refresh tokens for session management';

-- Authentication events audit log
CREATE TABLE auth_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100),
    event_type ENUM('LOGIN', 'LOGOUT', 'REFRESH', 'FAILED_LOGIN', 'PASSWORD_CHANGED', 'USER_CREATED', 'USER_UPDATED', 'USER_DISABLED') NOT NULL,
    success BOOLEAN NOT NULL,
    details VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_auth_events_username (username),
    INDEX idx_auth_events_type (event_type),
    INDEX idx_auth_events_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Authentication audit trail';
