-- ============================================================
-- BSS Telecom — Seed Data for All Microservice Databases
-- Run with: mysql -u root -proot < seed-data.sql
-- Or connect to Docker: docker exec -i billing-mysql mysql -u root -proot < seed-data.sql
-- ============================================================

SET NAMES 'utf8mb4';

-- ────────────────────────────────────────────────────────────
-- 1. CUSTOMER SERVICE (db_customer)
-- ────────────────────────────────────────────────────────────
CREATE DATABASE IF NOT EXISTS db_customer CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE db_customer;

-- Ensure gouvernorat column exists
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = 'db_customer' AND TABLE_NAME = 'client' AND COLUMN_NAME = 'gouvernorat');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE client ADD COLUMN gouvernorat VARCHAR(100) DEFAULT NULL', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Ensure piece_identite column exists (safe for MySQL 5.7+/8.0)
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = 'db_customer' AND TABLE_NAME = 'client' AND COLUMN_NAME = 'piece_identite');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE client ADD COLUMN piece_identite VARCHAR(20) DEFAULT NULL', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Clear existing data and reset auto-increment
DELETE FROM client;
ALTER TABLE client AUTO_INCREMENT = 1;

INSERT INTO client (customer_ref, boutique_ref, nom, prenom, email, telephone, piece_identite, adresse, ville, code_postal, gouvernorat, pays, type, status, account_balance, credit_limit, created_at, updated_at)
VALUES
  ('CLT-2024-001', '1', 'Ben Ali',       'Mohamed', 'mohamed.benali@email.tn',     '71234567', '09876543',       'Av. Habib Bourguiba, 15',       'Tunis',     '1000', 'Tunis',        'Tunisie', 'INDIVIDUAL', 'ACTIVE',     45.50,    200.00,  '2024-10-24 10:00:00', '2024-10-24 10:00:00'),
  ('CLT-2024-002', '1', 'Trabelsi',      'Amira',   'amira.trabelsi@email.tn',     '98765432', '12345678',       'Rue de la République, 42',      'Sfax',      '3000', 'Sfax',         'Tunisie', 'INDIVIDUAL', 'ACTIVE',      0.00,    150.00,  '2024-10-23 09:30:00', '2024-10-23 09:30:00'),
  ('CLT-2024-003', '1', 'Digital Solutions SARL', '',  'contact@digitalsol.tn',     '71456789', '1234567A/B/C/000','Zone Industrielle, Lot 8',     'Sousse',    '4000', 'Sousse',       'Tunisie', 'BUSINESS',   'ACTIVE',   1250.00,  5000.00,  '2024-10-23 14:00:00', '2024-10-23 14:00:00'),
  ('CLT-2024-004', '1', 'Gharbi',        'Youssef', 'youssef.gharbi@email.tn',     '55111222', '07654321',       'Cité El Khadra, Bloc 3',        'Tunis',     '1003', 'Tunis',        'Tunisie', 'INDIVIDUAL', 'SUSPENDED',  89.90,    200.00,  '2024-09-15 08:00:00', '2024-11-01 12:00:00'),
  ('CLT-2024-005', '2', 'TechnoServ SA',          '',  'admin@technoserv.tn',       '71888999', '9876543B/A/M/001','Centre Urbain Nord, Tour A',   'Ariana',    '2080', 'Ariana',       'Tunisie', 'BUSINESS',   'ACTIVE',   3420.00, 10000.00,  '2024-08-10 11:00:00', '2024-08-10 11:00:00'),
  ('CLT-2024-006', '1', 'Mansouri',      'Fatma',   'fatma.mansouri@email.tn',     '22333444', '11223344',       'Rue Ibn Khaldoun, 7',           'Monastir',  '5000', 'Monastir',     'Tunisie', 'INDIVIDUAL', 'ACTIVE',     12.30,    150.00,  '2024-11-02 16:00:00', '2024-11-02 16:00:00'),
  ('CLT-2024-007', '2', 'Hammami',       'Khaled',  'khaled.hammami@email.tn',     '55667788', '08811223',       'Av. de la Liberté, 22',         'Sfax',      '3000', 'Sfax',         'Tunisie', 'INDIVIDUAL', 'ACTIVE',     22.00,    200.00,  '2025-01-15 09:00:00', '2025-01-15 09:00:00'),
  ('CLT-2024-008', '1', 'Bouzid',        'Salma',   'salma.bouzid@email.tn',       '99887766', '09988776',       'Rue de Marseille, 5',           'Tunis',     '1002', 'Tunis',        'Tunisie', 'INDIVIDUAL', 'ACTIVE',      5.00,    150.00,  '2025-01-20 14:00:00', '2025-01-20 14:00:00'),
  ('CLT-2024-009', '2', 'MedTech SARL',           '',  'info@medtech.tn',           '74555666', '7654321C/A/P/000','Technopole de Sfax, Bloc B',  'Sfax',      '3021', 'Sfax',         'Tunisie', 'BUSINESS',   'ACTIVE',    800.00,  3000.00,  '2025-02-01 10:00:00', '2025-02-01 10:00:00'),
  ('CLT-2024-010', '1', 'Jebali',        'Nour',    'nour.jebali@email.tn',        '28112233', '06655443',       'Cité Olympique, Apt 12',        'Sousse',    '4000', 'Sousse',       'Tunisie', 'INDIVIDUAL', 'ACTIVE',      0.00,    200.00,  '2025-02-10 11:00:00', '2025-02-10 11:00:00');


-- ────────────────────────────────────────────────────────────
-- 2. CATALOG SERVICE (db_catalog) — Services + Offres
-- ────────────────────────────────────────────────────────────
CREATE DATABASE IF NOT EXISTS db_catalog CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE db_catalog;

-- Clear existing data and reset auto-increment
DELETE FROM offre_service;
DELETE FROM offre;
DELETE FROM service;
ALTER TABLE service AUTO_INCREMENT = 1;
ALTER TABLE offre AUTO_INCREMENT = 1;

-- Services (IDs will be 1..4)
INSERT INTO service (code, libelle, unite, prix_unitaire, category, active)
VALUES
  ('SVC_APPELS',  'Appels',          'SECONDE', 0.005, 'VOICE',   true),
  ('SVC_DATA',    'Données Mobiles', 'OCTET',   0.010, 'DATA',    true),
  ('SVC_SMS',     'SMS',             'SMS',     0.040, 'SMS',     true),
  ('SVC_ROAMING', 'Roaming',         'SECONDE', 0.090, 'ROAMING', true);

-- Offres (IDs will be 1..8)
INSERT INTO offre (code, libelle, description, prix_mensuel, date_debut, date_fin, status, payment_type)
VALUES
  ('FIBRE_20',   'Fibre Essentiel 20M',     'Connexion fibre optique 20 Mbps — idéale pour les particuliers. Inclut Wi-Fi et assistance 7j/7.', 35.00,  '2024-01-01', NULL, 'ACTIVE',   'POSTPAID'),
  ('FIBRE_100',  'Fibre Pro 100M',          'Fibre 100 Mbps dédiée aux professionnels avec IP fixe et SLA garanti 99.9%.',                      85.00,  '2024-01-01', NULL, 'ACTIVE',   'POSTPAID'),
  ('MOB_5G_ILL', 'Mobile 5G Illimité',      'Appels & SMS illimités + 100 Go data 5G. Roaming Maghreb inclus.',                                 75.00,  '2024-01-01', NULL, 'ACTIVE',   'POSTPAID'),
  ('MOB_4G_25',  'Forfait Mobile 4G 25 Go', 'Forfait prépayé 25 Go data 4G + 2h d''appels nationaux. Rechargeable en ligne.',                   19.90,  '2024-01-01', NULL, 'ACTIVE',   'PREPAID'),
  ('MOB_4G_10',  'Forfait Mobile 4G 10 Go', 'Forfait économique 10 Go data 4G + 1h d''appels. Idéal pour usage léger.',                         12.00,  '2024-01-01', NULL, 'ACTIVE',   'PREPAID'),
  ('PRO_CONV',   'Entreprise Convergent',   'Solution convergente Fibre 200 Mbps + 5 lignes mobiles + standard téléphonique IP.',               280.00,  '2024-01-01', NULL, 'ACTIVE',   'POSTPAID'),
  ('DATA_BOOST', 'Recharge Data 10 Go',     'Extension data 10 Go à activer en complément de votre forfait existant.',                            8.00,  '2024-06-01', NULL, 'ACTIVE',   'PREPAID'),
  ('ROAM_MAG',   'Pack Roaming Maghreb',    'Forfait roaming voix + data pour Algérie, Maroc et Libye. 5 Go + 3h d''appels.',                   45.00,  '2024-03-01', NULL, 'INACTIVE', 'PREPAID');

-- Offre ↔ Service associations
INSERT INTO offre_service (offre_id, service_id)
VALUES
  (1, 2),         -- Fibre Essentiel → Données Mobiles
  (2, 2),         -- Fibre Pro → Données Mobiles
  (3, 1), (3, 2), (3, 3), (3, 4),   -- Mobile 5G → Appels + Data + SMS + Roaming
  (4, 1), (4, 2), (4, 3),           -- Forfait 4G 25 → Appels + Data + SMS
  (5, 1), (5, 2), (5, 3),           -- Forfait 4G 10 → Appels + Data + SMS
  (6, 1), (6, 2), (6, 3),           -- Entreprise Convergent → Appels + Data + SMS
  (7, 2),                            -- Recharge Data → Données Mobiles
  (8, 4);                            -- Roaming Maghreb → Roaming


-- ────────────────────────────────────────────────────────────
-- 3. SUBSCRIPTION SERVICE (db_subscription)
-- ────────────────────────────────────────────────────────────
CREATE DATABASE IF NOT EXISTS db_subscription CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE db_subscription;

-- Clear existing data and reset auto-increment
DELETE FROM abonnement;
ALTER TABLE abonnement AUTO_INCREMENT = 1;

INSERT INTO abonnement (client_id, client_ref, offre_id, date_debut, date_fin, status, billing_frequency, last_billing_date, next_billing_date, created_at, updated_at)
VALUES
  (1, 'CLT-2024-001', 4, '2024-10-24', NULL, 'ACTIVE',     'MONTHLY', '2025-01-01', '2025-02-01', '2024-10-24 10:00:00', '2024-10-24 10:00:00'),
  (1, 'CLT-2024-001', 7, '2024-11-01', NULL, 'ACTIVE',     'MONTHLY', '2025-01-01', '2025-02-01', '2024-11-01 09:00:00', '2024-11-01 09:00:00'),
  (2, 'CLT-2024-002', 5, '2024-10-23', NULL, 'ACTIVE',     'MONTHLY', '2025-01-01', '2025-02-01', '2024-10-23 09:30:00', '2024-10-23 09:30:00'),
  (3, 'CLT-2024-003', 6, '2024-10-23', NULL, 'ACTIVE',    'QUARTERLY','2025-01-01', '2025-04-01', '2024-10-23 14:00:00', '2024-10-23 14:00:00'),
  (3, 'CLT-2024-003', 2, '2024-10-25', NULL, 'ACTIVE',    'QUARTERLY','2025-01-01', '2025-04-01', '2024-10-25 11:00:00', '2024-10-25 11:00:00'),
  (4, 'CLT-2024-004', 3, '2024-09-15', NULL, 'SUSPENDED',  'MONTHLY', '2024-11-01', '2024-12-01', '2024-09-15 08:00:00', '2024-11-01 12:00:00'),
  (5, 'CLT-2024-005', 6, '2024-08-10', NULL, 'ACTIVE',     'ANNUAL',  '2025-01-01', '2026-01-01', '2024-08-10 11:00:00', '2024-08-10 11:00:00'),
  (6, 'CLT-2024-006', 4, '2024-11-02', NULL, 'ACTIVE',     'MONTHLY', '2025-01-01', '2025-02-01', '2024-11-02 16:00:00', '2024-11-02 16:00:00'),
  (7, 'CLT-2024-007', 3, '2025-01-15', NULL, 'ACTIVE',     'MONTHLY', '2025-02-01', '2025-03-01', '2025-01-15 09:00:00', '2025-01-15 09:00:00'),
  (8, 'CLT-2024-008', 5, '2025-01-20', NULL, 'ACTIVE',     'MONTHLY', '2025-02-01', '2025-03-01', '2025-01-20 14:00:00', '2025-01-20 14:00:00'),
  (10,'CLT-2024-010', 1, '2025-02-10', NULL, 'ACTIVE',     'MONTHLY', '2025-03-01', '2025-04-01', '2025-02-10 11:00:00', '2025-02-10 11:00:00'),
  (1,  'CLT-2024-001', 3, '2026-06-02', NULL, 'ACTIVE',     'MONTHLY',  NULL, '2026-07-02', '2026-06-02 09:00:00', '2026-06-02 09:00:00'),
  (2,  'CLT-2024-002', 3, '2026-06-02', NULL, 'ACTIVE',     'MONTHLY',  NULL, '2026-07-02', '2026-06-02 10:30:00', '2026-06-02 10:30:00');


-- ────────────────────────────────────────────────────────────
-- 4. BILLING SERVICE (db_billing)
-- ────────────────────────────────────────────────────────────
CREATE DATABASE IF NOT EXISTS db_billing CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE db_billing;

-- Clear existing data and reset auto-increment
DELETE FROM invoice_line;
DELETE FROM facture;
ALTER TABLE invoice_line AUTO_INCREMENT = 1;
ALTER TABLE facture AUTO_INCREMENT = 1;

INSERT INTO facture (numero_facture, client_id, abonnement_id, date_facture, date_echeance, periode_debut, periode_fin, montant_ht, montant_tva, montant_ttc, statut, created_at, updated_at, paid_at)
VALUES
  ('FAC-2025-0001', 1, 1, '2025-01-05', '2025-02-04', '2024-12-01', '2024-12-31', 19.90, 0.00, 19.90, 'PAID',    '2025-01-05 08:00:00', '2025-01-15 10:00:00', '2025-01-15 10:00:00'),
  ('FAC-2025-0002', 1, 2, '2025-01-05', '2025-02-04', '2024-12-01', '2024-12-31',  8.00, 0.00,  8.00, 'PAID',    '2025-01-05 08:00:00', '2025-01-15 10:00:00', '2025-01-15 10:00:00'),
  ('FAC-2025-0003', 2, 3, '2025-01-05', '2025-02-04', '2024-12-01', '2024-12-31', 12.00, 0.00, 12.00, 'PAID',    '2025-01-05 08:00:00', '2025-01-20 14:00:00', '2025-01-20 14:00:00'),
  ('FAC-2025-0004', 3, 4, '2025-01-05', '2025-02-04', '2024-12-01', '2024-12-31',280.00, 0.00,280.00, 'PENDING', '2025-01-05 08:00:00', '2025-01-05 08:00:00', NULL),
  ('FAC-2025-0005', 3, 5, '2025-01-05', '2025-02-04', '2024-12-01', '2024-12-31', 85.00, 0.00, 85.00, 'PENDING', '2025-01-05 08:00:00', '2025-01-05 08:00:00', NULL),
  ('FAC-2025-0006', 4, 6, '2024-11-05', '2024-12-05', '2024-10-01', '2024-10-31', 75.00, 0.00, 75.00, 'OVERDUE', '2024-11-05 08:00:00', '2024-11-05 08:00:00', NULL),
  ('FAC-2025-0007', 5, 7, '2025-01-05', '2025-02-04', '2024-12-01', '2024-12-31',280.00, 0.00,280.00, 'PAID',    '2025-01-05 08:00:00', '2025-01-10 09:00:00', '2025-01-10 09:00:00'),
  ('FAC-2025-0008', 6, 8, '2025-01-05', '2025-02-04', '2024-12-01', '2024-12-31', 19.90, 0.00, 19.90, 'SENT',    '2025-01-05 08:00:00', '2025-01-05 08:00:00', NULL),
  ('FAC-2026-0001', 1, 1, '2026-06-02', '2026-07-02', '2026-05-01', '2026-05-31', 75.00, 0.00, 75.00, 'PENDING', '2026-06-02 09:00:00', '2026-06-02 09:00:00', NULL);

-- Invoice lines
INSERT INTO invoice_line (facture_id, type, description, service_id, quantite, prix_unitaire, montant)
VALUES
  (1, 'SUBSCRIPTION', 'Forfait Mobile 4G 25 Go — Janvier 2025', NULL, 1, 19.90, 19.90),
  (2, 'SUBSCRIPTION', 'Recharge Data 10 Go — Janvier 2025',     NULL, 1,  8.00,  8.00),
  (3, 'SUBSCRIPTION', 'Forfait Mobile 4G 10 Go — Janvier 2025', NULL, 1, 12.00, 12.00),
  (4, 'SUBSCRIPTION', 'Entreprise Convergent — Janvier 2025',   NULL, 1, 280.00, 280.00),
  (5, 'SUBSCRIPTION', 'Fibre Pro 100M — Janvier 2025',          NULL, 1, 85.00, 85.00),
  (6, 'SUBSCRIPTION', 'Mobile 5G Illimité — Novembre 2024',     NULL, 1, 75.00, 75.00),
  (7, 'SUBSCRIPTION', 'Entreprise Convergent — Janvier 2025',   NULL, 1, 280.00, 280.00),
  (8, 'SUBSCRIPTION', 'Forfait Mobile 4G 25 Go — Janvier 2025', NULL, 1, 19.90, 19.90);


-- ────────────────────────────────────────────────────────────
-- 5. PAYMENT SERVICE (db_payment)
-- ────────────────────────────────────────────────────────────
CREATE DATABASE IF NOT EXISTS db_payment CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE db_payment;

-- Ensure payment table exists even if payment-service has not been started yet
CREATE TABLE IF NOT EXISTS payment (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  idempotency_key VARCHAR(255) NOT NULL,
  reference VARCHAR(100) NOT NULL,
  client_id BIGINT NOT NULL,
  facture_id BIGINT NULL,
  montant DECIMAL(10,2) NOT NULL,
  methode_paiement VARCHAR(50) NOT NULL,
  statut VARCHAR(50) NOT NULL,
  transaction_id VARCHAR(255) NULL,
  created_at DATETIME NOT NULL,
  processed_at DATETIME NULL,
  UNIQUE KEY uk_payment_idempotency_key (idempotency_key),
  UNIQUE KEY uk_payment_reference (reference)
);

-- Clear existing data and reset auto-increment
DELETE FROM payment;
ALTER TABLE payment AUTO_INCREMENT = 1;

INSERT INTO payment (idempotency_key, reference, client_id, facture_id, montant, methode_paiement, statut, transaction_id, created_at, processed_at)
VALUES
  ('idem-001', 'PAY-2025-0001', 1, 1, 19.90, 'BANK_TRANSFER', 'COMPLETED', 'TXN-VIR-001', '2025-01-15 10:00:00', '2025-01-15 10:00:00'),
  ('idem-002', 'PAY-2025-0002', 1, 2,  8.00, 'CREDIT_CARD',   'COMPLETED', 'TXN-CB-001',  '2025-01-15 10:05:00', '2025-01-15 10:05:00'),
  ('idem-003', 'PAY-2025-0003', 2, 3, 12.00, 'CASH',          'COMPLETED', NULL,           '2025-01-20 14:00:00', '2025-01-20 14:00:00'),
  ('idem-004', 'PAY-2025-0004', 5, 7, 280.00, 'BANK_TRANSFER','COMPLETED', 'TXN-VIR-002', '2025-01-10 09:00:00', '2025-01-10 09:00:00');


-- ────────────────────────────────────────────────────────────
-- 6. AUTHENTICATION SERVICE (db_authentication)
-- ────────────────────────────────────────────────────────────
CREATE DATABASE IF NOT EXISTS db_authentication CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE db_authentication;

-- Default users (password: admin123 — BCrypt hash)
DELETE FROM auth_events;
DELETE FROM refresh_tokens;
DELETE FROM users;
ALTER TABLE users AUTO_INCREMENT = 1;

INSERT INTO users (username, password_hash, first_name, last_name, role, status, boutique_id, created_at)
VALUES
  ('admin',       '$2a$12$AFCZHjzjWPWq59nBjGkL3uVAtMH6s.46aAT1K.w7XCJEwwImQXbAW', 'Admin',   'Système',    'ADMIN',                'ACTIVE', NULL, '2024-01-01 00:00:00'),
  ('bouaziz.sami','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Sami',    'Bouaziz',    'AGENT_COMMERCIAL',     'ACTIVE', 1,    '2024-01-15 08:00:00'),
  ('hammami.nadia','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy','Nadia',   'Hammami',    'RESPONSABLE_BOUTIQUE', 'ACTIVE', 1,    '2024-01-01 00:00:00'),
  ('mejri.ahmed', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Ahmed',   'Mejri',      'AGENT_COMMERCIAL',     'ACTIVE', 1,    '2024-03-10 09:00:00'),
  ('khelifi.ines','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Ines',    'Khelifi',    'AGENT_COMMERCIAL',     'ACTIVE', 2,    '2024-04-01 08:00:00'),
  ('chabbi.rami', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Rami',    'Chabbi',     'RESPONSABLE_BOUTIQUE', 'ACTIVE', 2,    '2024-04-01 08:00:00'),
  ('jlassi.amal', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Amal',    'Jlassi',     'AGENT_COMMERCIAL',     'DISABLED', 1,  '2024-02-20 08:00:00'),
  ('responsable', '$2a$12$AFCZHjzjWPWq59nBjGkL3uVAtMH6s.46aAT1K.w7XCJEwwImQXbAW', 'Demo',    'Responsable','RESPONSABLE_BOUTIQUE', 'ACTIVE', 1,    '2026-03-10 00:00:00'),
  ('agent',       '$2a$12$AFCZHjzjWPWq59nBjGkL3uVAtMH6s.46aAT1K.w7XCJEwwImQXbAW', 'Demo',    'Agent',      'AGENT_COMMERCIAL',     'ACTIVE', 1,    '2026-03-10 00:00:00')
ON DUPLICATE KEY UPDATE username = VALUES(username);


-- ────────────────────────────────────────────────────────────
-- 7. BOUTIQUE SERVICE (db_boutique)
-- ────────────────────────────────────────────────────────────
CREATE DATABASE IF NOT EXISTS db_boutique CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE db_boutique;

-- Clear existing data and reset auto-increment
DELETE FROM transaction_boutique;
DELETE FROM stock_sim;
DELETE FROM boutique;
ALTER TABLE transaction_boutique AUTO_INCREMENT = 1;
ALTER TABLE stock_sim AUTO_INCREMENT = 1;
ALTER TABLE boutique AUTO_INCREMENT = 1;

INSERT INTO boutique (code, nom, adresse, ville, code_postal, telephone, email, responsable_id, status, created_at)
VALUES
  ('BTQ-TUNIS-01',  'Boutique Tunis Centre',   'Av. Habib Bourguiba, 28',  'Tunis',    '1000', '+216 71 300 100', 'tunis.centre@telecom.tn',    3, 'ACTIVE', '2024-06-01 09:00:00'),
  ('BTQ-SFAX-01',   'Boutique Sfax Médina',    'Rue de la République, 5',  'Sfax',     '3000', '+216 74 200 200', 'sfax.medina@telecom.tn',     6, 'ACTIVE', '2024-07-15 09:00:00'),
  ('BTQ-SOUSSE-01', 'Boutique Sousse Marina',  'Port El Kantaoui, Bloc 2', 'Sousse',   '4000', '+216 73 100 300', 'sousse.marina@telecom.tn',NULL, 'ACTIVE', '2024-08-01 09:00:00');

-- SIM stock for Boutique Tunis Centre (id=1)
INSERT INTO stock_sim (iccid, imsi, msisdn, sim_type, status, boutique_id, assigned_to_client_id, assigned_at, created_at)
VALUES
  ('8921600100000001', '605010100000001', '+21650000001', 'STANDARD', 'ACTIVATED',  1, 1, '2024-10-24 10:30:00', '2024-09-01 10:00:00'),
  ('8921600100000002', '605010100000002', '+21650000002', 'STANDARD', 'ACTIVATED',  1, 2, '2024-10-23 09:45:00', '2024-09-01 10:00:00'),
  ('8921600100000003', '605010100000003', NULL,           'STANDARD', 'AVAILABLE',  1, NULL, NULL, '2024-09-01 10:00:00'),
  ('8921600100000004', '605010100000004', NULL,           'STANDARD', 'AVAILABLE',  1, NULL, NULL, '2024-09-01 10:00:00'),
  ('8921600100000005', '605010100000005', NULL,           'ESIM',     'AVAILABLE',  1, NULL, NULL, '2024-09-01 10:00:00'),
  ('8921600100000006', '605010100000006', NULL,           'ESIM',     'AVAILABLE',  1, NULL, NULL, '2024-09-01 10:00:00'),
  ('8921600100000007', '605010100000007', NULL,           'ESIM',     'AVAILABLE',  1, NULL, NULL, '2024-10-15 14:00:00'),
  ('8921600100000008', '605010100000008', '+21650000008', 'STANDARD', 'ASSIGNED',   1, 4, '2024-09-15 08:30:00', '2024-09-01 10:00:00'),
  ('8921600100000009', NULL,              NULL,           'STANDARD', 'DAMAGED',    1, NULL, NULL, '2024-09-01 10:00:00'),
  ('8921600100000010', '605010100000010', '+21650000010', 'STANDARD', 'ACTIVATED',  1, 3, '2024-10-23 14:15:00', '2024-09-01 10:00:00');

-- SIM stock for Boutique Sfax (id=2)
INSERT INTO stock_sim (iccid, imsi, msisdn, sim_type, status, boutique_id, created_at)
VALUES
  ('8921600200000001', '605010200000001', NULL, 'STANDARD', 'AVAILABLE', 2, '2024-09-15 10:00:00'),
  ('8921600200000002', '605010200000002', NULL, 'STANDARD', 'AVAILABLE', 2, '2024-09-15 10:00:00'),
  ('8921600200000003', '605010200000003', NULL, 'ESIM',     'AVAILABLE', 2, '2024-09-15 10:00:00'),
  ('8921600200000004', '605010200000004', NULL, 'ESIM',     'AVAILABLE', 2, '2024-09-15 10:00:00');

-- Transactions for Boutique Tunis Centre (id=1)
INSERT INTO transaction_boutique (reference, boutique_id, agent_id, client_id, client_nom, offre_libelle, type_transaction, montant, status, created_at)
VALUES
  ('TXN-BTQ-2025-001', 1, 2, 1, 'Mohamed Ben Ali',    'Forfait Mobile 4G 25 Go',  'NEW_SUBSCRIPTION',  19.90, 'COMPLETED', '2025-02-16 09:15:00'),
  ('TXN-BTQ-2025-002', 1, 2, 6, 'Fatma Mansouri',     'Forfait Mobile 4G 25 Go',  'NEW_SUBSCRIPTION',  19.90, 'COMPLETED', '2025-02-16 10:30:00'),
  ('TXN-BTQ-2025-003', 1, 2, 5, 'TechnoServ SA',      'Entreprise Convergent',    'RENEWAL',          280.00, 'COMPLETED', '2025-02-16 14:00:00'),
  ('TXN-BTQ-2025-004', 1, 2, 2, 'Amira Trabelsi',     'Facture FAC-2025-0003',    'INVOICE_PAYMENT',   12.00, 'COMPLETED', '2025-02-16 15:30:00'),
  ('TXN-BTQ-2025-005', 1, 2, 4, 'Youssef Gharbi',     'Forfait Mobile 4G 25 Go',  'CANCELLATION',      19.90, 'COMPLETED', '2025-02-16 16:00:00'),
  ('TXN-BTQ-2026-001', 1, 2, 1, 'Mohamed Ben Ali',    'Mobile 5G Illimité',       'NEW_SUBSCRIPTION',  75.00, 'COMPLETED', '2026-06-02 09:05:00'),
  ('TXN-BTQ-2026-002', 1, 2, 2, 'Amira Trabelsi',     'Mobile 5G Illimité',       'NEW_SUBSCRIPTION',  75.00, 'COMPLETED', '2026-06-02 10:45:00'),
  ('TXN-BTQ-2026-003', 2, 2, 7, 'Khaled Hammami',     'Forfait Mobile 4G 10 Go',  'RECHARGE',          12.00, 'COMPLETED', '2026-06-02 11:20:00'),
  ('TXN-BTQ-2026-004', 1, 2, 8, 'Salma Bouzid',       'Recharge Data 10 Go',      'RECHARGE',           8.00, 'COMPLETED', '2026-06-02 14:10:00');


