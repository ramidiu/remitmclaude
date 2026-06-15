-- =============================================================================
-- ROLLBACK SCRIPT — Removes ALL migrated data from remitm.
--                   Restores the database to its pre-migration state.
--
-- !! USE WITH EXTREME CAUTION — THIS DELETES DATA !!
-- Only run this if the migration produced incorrect results and you need
-- to start fresh. The 6 seed/admin users and original test transactions
-- are preserved (id <= 6 is never touched).
--
-- Execute inside container:
--   docker exec -i remitm-mysql mysql -uroot -proot remitm < 06_rollback.sql
-- =============================================================================

USE remitm;

SET SESSION foreign_key_checks = 0;
SET SESSION sql_mode = '';

SELECT '!!! ROLLBACK STARTED — removing all migrated data !!!' AS warning;

-- 1. Remove transaction_status_history for migrated transactions
DELETE tsh FROM remitm.transaction_status_history tsh
JOIN remitm.transactions t ON tsh.transaction_id = t.id
WHERE t.reference_number LIKE 'TXN%';
SELECT CONCAT('Deleted transaction_status_history rows: ', ROW_COUNT()) AS step_1;

-- 2. Remove migrated transactions
DELETE FROM remitm.transactions
WHERE reference_number LIKE 'TXN%';
SELECT CONCAT('Deleted transactions: ', ROW_COUNT()) AS step_2;

-- 3. Remove beneficiary mapping
DELETE FROM remitm.migration_beneficiary_map WHERE old_beneficiary_id > 0;
SELECT CONCAT('Cleared migration_beneficiary_map: ', ROW_COUNT()) AS step_3;

-- 4. Remove migrated beneficiaries (those whose user_id is in the migration map)
DELETE b FROM remitm.beneficiaries b
JOIN remitm.migration_user_map m ON b.user_id = m.new_user_id;
SELECT CONCAT('Deleted beneficiaries: ', ROW_COUNT()) AS step_4;

-- 5. Remove kyc_verifications for migrated users
DELETE kv FROM remitm.kyc_verifications kv
JOIN remitm.migration_user_map m ON kv.user_id = m.new_user_id;
SELECT CONCAT('Deleted kyc_verifications: ', ROW_COUNT()) AS step_5;

-- 6. Remove kyc_documents for migrated users
DELETE kd FROM remitm.kyc_documents kd
JOIN remitm.migration_user_map m ON kd.user_id = m.new_user_id;
SELECT CONCAT('Deleted kyc_documents: ', ROW_COUNT()) AS step_6;

-- 7. Remove wallets for migrated users
DELETE w FROM remitm.wallets w
JOIN remitm.migration_user_map m ON w.user_id = m.new_user_id;
SELECT CONCAT('Deleted wallets: ', ROW_COUNT()) AS step_7;

-- 8. Remove migrated users (id > 6 only — never touch seed users)
DELETE FROM remitm.users
WHERE id > 6
  AND id IN (SELECT new_user_id FROM remitm.migration_user_map);
SELECT CONCAT('Deleted users: ', ROW_COUNT()) AS step_8;

-- 9. Clear migration tracking tables
DELETE FROM remitm.migration_user_map WHERE id > 0;
SELECT CONCAT('Cleared migration_user_map: ', ROW_COUNT()) AS step_9;

-- 10. Clear remit_one (fully migrated table)
TRUNCATE TABLE remitm.remit_one;
SELECT 'Truncated remit_one' AS step_10;

-- 11. Clear migration log and summary
TRUNCATE TABLE remitm.migration_log;
TRUNCATE TABLE remitm.migration_summary;
SELECT 'Cleared migration_log and migration_summary' AS step_11;

SET SESSION foreign_key_checks = 1;

-- Verify state
SELECT
    'POST-ROLLBACK STATE' AS report,
    (SELECT COUNT(*) FROM remitm.users) AS users_remaining,
    (SELECT COUNT(*) FROM remitm.transactions) AS transactions_remaining,
    (SELECT COUNT(*) FROM remitm.beneficiaries) AS beneficiaries_remaining,
    (SELECT COUNT(*) FROM remitm.kyc_documents) AS kyc_docs_remaining,
    (SELECT COUNT(*) FROM remitm.remit_one) AS remit_one_remaining,
    (SELECT COUNT(*) FROM remitm.migration_user_map) AS user_map_remaining;

SELECT 'ROLLBACK COMPLETE — database restored to pre-migration state.' AS status;
