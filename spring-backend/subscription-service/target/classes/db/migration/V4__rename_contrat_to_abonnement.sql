
ALTER TABLE quota DROP CONSTRAINT fk_quota_contrat;
ALTER TABLE quota DROP INDEX uk_quota_contrat_type;
ALTER TABLE quota DROP INDEX idx_quota_contrat;

ALTER TABLE quota CHANGE COLUMN contrat_id abonnement_id BIGINT NOT NULL;

ALTER TABLE contrat RENAME TO abonnement;

ALTER TABLE quota ADD CONSTRAINT fk_quota_abonnement FOREIGN KEY (abonnement_id) REFERENCES abonnement(id);
ALTER TABLE quota ADD CONSTRAINT uk_quota_abonnement_type UNIQUE (abonnement_id, quota_type);
ALTER TABLE quota ADD INDEX idx_quota_abonnement (abonnement_id);

ALTER TABLE abonnement DROP INDEX idx_contrat_billing;
ALTER TABLE abonnement ADD INDEX idx_abonnement_billing (status, next_billing_date);

ALTER TABLE abonnement COMMENT = 'Subscriptions/Abonnements with billing frequency support';
