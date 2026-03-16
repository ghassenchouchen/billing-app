-- Customer Service - Schema

CREATE TABLE IF NOT EXISTS client (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_ref VARCHAR(36) NOT NULL UNIQUE,
    boutique_ref VARCHAR(255) NOT NULL,
    nom VARCHAR(255) NOT NULL,
    prenom VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    telephone VARCHAR(255),
    piece_identite VARCHAR(20),
    adresse VARCHAR(255),
    ville VARCHAR(255),
    code_postal VARCHAR(255),
    pays VARCHAR(255),
    type VARCHAR(50) NOT NULL,  -- INDIVIDUAL or BUSINESS
    status VARCHAR(50) NOT NULL,  -- ACTIVE, SUSPENDED, CLOSED
    account_balance DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    credit_limit DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    INDEX idx_customer_ref (customer_ref)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
