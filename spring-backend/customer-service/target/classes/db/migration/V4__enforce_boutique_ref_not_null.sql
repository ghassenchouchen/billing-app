-- Enforce boutique_ref integrity in existing databases
UPDATE client
SET boutique_ref = '1'
WHERE boutique_ref IS NULL OR TRIM(boutique_ref) = '';

ALTER TABLE client
MODIFY COLUMN boutique_ref VARCHAR(255) NOT NULL;
