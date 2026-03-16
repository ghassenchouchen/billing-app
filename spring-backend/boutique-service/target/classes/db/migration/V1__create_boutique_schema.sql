

-- Boutique table (shop/store locations)
CREATE TABLE boutique (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    nom VARCHAR(255) NOT NULL,
    adresse VARCHAR(255) NOT NULL,
    ville VARCHAR(100) NOT NULL,
    code_postal VARCHAR(10) NOT NULL,
    telephone VARCHAR(20) NOT NULL,
    email VARCHAR(100) NOT NULL,
    responsable_id BIGINT NULL,
    status ENUM('ACTIVE', 'INACTIVE', 'CLOSED') NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_boutique_code (code),
    INDEX idx_boutique_status (status),
    INDEX idx_boutique_responsable (responsable_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- SIM Stock inventory
CREATE TABLE stock_sim (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    iccid VARCHAR(20) NOT NULL UNIQUE,
    imsi VARCHAR(15) NULL UNIQUE,
    msisdn VARCHAR(20) NULL UNIQUE,
    sim_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    boutique_id BIGINT NOT NULL,
    assigned_to_client_id BIGINT NULL,
    assigned_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_stock_sim_boutique FOREIGN KEY (boutique_id) REFERENCES boutique(id) ON DELETE CASCADE,
    INDEX idx_stock_sim_iccid (iccid),
    INDEX idx_stock_sim_status (status),
    INDEX idx_stock_sim_boutique (boutique_id),
    INDEX idx_stock_sim_client (assigned_to_client_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Boutique transactions 
CREATE TABLE transaction_boutique (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reference VARCHAR(50) NOT NULL UNIQUE,
    boutique_id BIGINT NOT NULL,
    agent_id BIGINT NULL,
    client_id BIGINT NULL,
    client_nom VARCHAR(255) NULL,
    offre_libelle VARCHAR(255) NULL,
    type_transaction VARCHAR(50) NOT NULL,
    montant DECIMAL(10, 2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_transaction_boutique FOREIGN KEY (boutique_id) REFERENCES boutique(id) ON DELETE CASCADE,
    INDEX idx_transaction_type (type_transaction),
    INDEX idx_transaction_boutique (boutique_id),
    INDEX idx_transaction_status (status),
    INDEX idx_transaction_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
