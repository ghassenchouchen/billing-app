-- Quota table for prepaid subscription bundles
CREATE TABLE IF NOT EXISTS quota (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    abonnement_id BIGINT NOT NULL,
    quota_type VARCHAR(20) NOT NULL,
    total_amount DECIMAL(15,4) NOT NULL,
    remaining_amount DECIMAL(15,4) NOT NULL,
    unit VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    CONSTRAINT fk_quota_abonnement FOREIGN KEY (abonnement_id) REFERENCES abonnement(id),
    CONSTRAINT uk_quota_abonnement_type UNIQUE (abonnement_id, quota_type),
    INDEX idx_quota_abonnement (abonnement_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
