USE db_subscription;

ALTER TABLE abonnement
  MODIFY COLUMN client_id BIGINT NULL,
  ADD COLUMN client_ref VARCHAR(64) NULL AFTER client_id;

CREATE INDEX idx_abonnement_client_ref ON abonnement(client_ref);
