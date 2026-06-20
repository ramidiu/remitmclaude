-- =============================================================================
-- STEP 14 — Post-migration data corrections surfaced by the FCA export:
--   1. transactions.sender_email (and sender_name) were never denormalised onto
--      the transaction row (migration only linked sender_id) → export showed
--      Sender Email blank. Backfill from the linked customer.
--   2. Mobile-money payouts were mislabeled delivery_method='BANK_DEPOSIT'
--      (PHASE 7 defaulted unmapped legacy payment types to BANK_DEPOSIT). For
--      beneficiaries that are clearly mobile-money (have a mobile provider, no
--      bank account), correct them to MOBILE_WALLET.
-- updated_at is preserved (set to itself) so completion dates don't get re-bumped.
-- =============================================================================
USE remitm;
SET SESSION sql_mode = '';

-- 1. backfill sender_email / sender_name from the customer where missing
UPDATE transactions t JOIN users u ON u.id = t.sender_id
SET t.sender_email = COALESCE(NULLIF(TRIM(t.sender_email), ''), u.email),
    t.sender_name  = COALESCE(NULLIF(TRIM(t.sender_name), ''),
                              NULLIF(TRIM(CONCAT_WS(' ', u.first_name, u.last_name)), '')),
    t.updated_at   = t.updated_at
WHERE t.sender_email IS NULL OR TRIM(t.sender_email) = ''
   OR t.sender_name  IS NULL OR TRIM(t.sender_name) = '';

-- 2. fix delivery_method for mobile-money recipients mislabeled BANK_DEPOSIT
UPDATE transactions t JOIN beneficiaries b ON b.id = t.beneficiary_id
SET t.delivery_method = 'MOBILE_WALLET',
    t.updated_at = t.updated_at
WHERE t.delivery_method = 'BANK_DEPOSIT'
  AND b.mobile_provider IS NOT NULL AND TRIM(b.mobile_provider) <> ''
  AND (b.bank_name IS NULL OR TRIM(b.bank_name) = '')
  AND (b.account_number IS NULL OR TRIM(b.account_number) = '');
