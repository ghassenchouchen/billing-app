USE db_subscription;

-- Populate missing client_ref for existing abonnements that have a client_id
-- This ensures backward compatibility with older subscription records
UPDATE abonnement
SET client_ref = CONCAT('CLT-2024-', LPAD(id, 6, '0'))
WHERE client_ref IS NULL OR client_ref = '';

-- Log migration status
SELECT 
    COUNT(*) as total_abonnements,
    SUM(CASE WHEN client_ref IS NULL OR client_ref = '' THEN 1 ELSE 0 END) as missing_refs,
    SUM(CASE WHEN client_ref IS NOT NULL AND client_ref != '' THEN 1 ELSE 0 END) as populated_refs
FROM abonnement;
