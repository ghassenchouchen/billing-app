
USE db_subscription;

ALTER TABLE contrat 
ADD COLUMN billing_frequency VARCHAR(20) NOT NULL DEFAULT 'MONTHLY' AFTER status,
ADD COLUMN last_billing_date DATE NULL AFTER billing_frequency,
ADD COLUMN next_billing_date DATE NULL AFTER last_billing_date;


UPDATE contrat
SET next_billing_date = DATE_ADD(date_debut, INTERVAL 1 MONTH)
WHERE next_billing_date IS NULL 
  AND billing_frequency = 'MONTHLY'
  AND status = 'ACTIVE';

CREATE INDEX idx_abonnement_billing ON contrat(status, next_billing_date);

ALTER TABLE contrat COMMENT = 'Subscriptions/Abonnements with billing frequency support';
