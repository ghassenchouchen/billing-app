-- Cross-service legacy reference reconciliation
-- Run manually once against the same MySQL server hosting all service databases.
-- Targets:
-- 1) db_customer.client.boutique_ref backfill
-- 2) db_subscription.abonnement.client_ref backfill from db_customer
-- 3) discrepancy checks for orphan references in subscription and boutique data

SET SQL_SAFE_UPDATES = 0;

-- --------------------------------------------------------------------------
-- A) CUSTOMER: backfill missing boutique_ref
-- --------------------------------------------------------------------------
SELECT COUNT(*) AS missing_customer_boutique_ref_before
FROM db_customer.client
WHERE boutique_ref IS NULL OR TRIM(boutique_ref) = '';

UPDATE db_customer.client
SET boutique_ref = CASE
    WHEN LOWER(TRIM(COALESCE(ville, ''))) IN ('sfax', 'sakiet ezzit', 'sakiet eddaier', 'mharza', 'el ain') THEN '2'
    WHEN LOWER(TRIM(COALESCE(ville, ''))) IN ('sousse', 'monastir', 'mahdia', 'kairouan', 'moknine') THEN '3'
    ELSE '1'
END
WHERE boutique_ref IS NULL OR TRIM(boutique_ref) = '';

SELECT COUNT(*) AS missing_customer_boutique_ref_after
FROM db_customer.client
WHERE boutique_ref IS NULL OR TRIM(boutique_ref) = '';

-- --------------------------------------------------------------------------
-- B) SUBSCRIPTION: backfill missing client_ref from customer table by client_id
-- --------------------------------------------------------------------------
SELECT COUNT(*) AS missing_subscription_client_ref_before
FROM db_subscription.abonnement a
WHERE (a.client_ref IS NULL OR TRIM(a.client_ref) = '')
  AND a.client_id IS NOT NULL;

UPDATE db_subscription.abonnement a
JOIN db_customer.client c ON c.id = a.client_id
SET a.client_ref = c.customer_ref
WHERE (a.client_ref IS NULL OR TRIM(a.client_ref) = '')
  AND a.client_id IS NOT NULL;

SELECT COUNT(*) AS missing_subscription_client_ref_after
FROM db_subscription.abonnement a
WHERE (a.client_ref IS NULL OR TRIM(a.client_ref) = '')
  AND a.client_id IS NOT NULL;

-- --------------------------------------------------------------------------
-- C) DISCREPANCY CHECKS (report-only)
-- --------------------------------------------------------------------------

-- Subscriptions with client_id not found in customer table
SELECT COUNT(*) AS orphan_subscription_client_id
FROM db_subscription.abonnement a
LEFT JOIN db_customer.client c ON c.id = a.client_id
WHERE a.client_id IS NOT NULL AND c.id IS NULL;

-- Subscriptions with client_ref not found in customer table
SELECT COUNT(*) AS orphan_subscription_client_ref
FROM db_subscription.abonnement a
LEFT JOIN db_customer.client c ON c.customer_ref = a.client_ref
WHERE a.client_ref IS NOT NULL AND TRIM(a.client_ref) <> '' AND c.customer_ref IS NULL;

-- SIM assignments pointing to missing customers
SELECT COUNT(*) AS orphan_stock_sim_customer_link
FROM db_boutique.stock_sim s
LEFT JOIN db_customer.client c ON c.id = s.assigned_to_client_id
WHERE s.assigned_to_client_id IS NOT NULL AND c.id IS NULL;

-- Boutique transactions pointing to missing customers
SELECT COUNT(*) AS orphan_boutique_transaction_customer_link
FROM db_boutique.transaction_boutique t
LEFT JOIN db_customer.client c ON c.id = t.client_id
WHERE t.client_id IS NOT NULL AND c.id IS NULL;
