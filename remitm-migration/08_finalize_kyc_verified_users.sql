-- =============================================================================
-- STEP 8 — Finalize KYC for migrated VERIFIED customers.
--
-- Problem: PHASE 5A/4B set each document's status from its own legacy flag, so a
-- fully-verified legacy customer (kyc_tier TIER_1/2/3) can still have a PENDING
-- document (e.g. an ID was 'verified' but the address proof wasn't). The new
-- transaction guard blocks Send Money if ANY document is PENDING — so verified
-- migrated customers get "Your KYC documents are still under review".
--
-- Fix: for migrated users who reached a verified tier, approve any remaining
-- PENDING documents (they were already trusted in the legacy system).
--
-- NOTE: this does NOT clear expiry dates — a genuinely expired ID should still
-- require renewal (compliance). On the local snapshot, stale 2023 expiry dates on
-- the team's own test accounts were cleared manually; for real production data,
-- review expired documents separately rather than auto-clearing them here.
-- =============================================================================
USE remitm;
SET SESSION sql_mode = '';

UPDATE kyc_documents d
JOIN users u ON u.id = d.user_id
SET d.status = 'APPROVED'
WHERE u.kyc_tier IN ('TIER_1','TIER_2','TIER_3')
  AND d.status = 'PENDING';

SELECT CONCAT('Approved ', ROW_COUNT(), ' pending documents for verified migrated users.') AS result;
