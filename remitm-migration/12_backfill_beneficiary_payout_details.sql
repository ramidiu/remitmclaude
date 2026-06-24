-- =============================================================================
-- STEP 12 — Backfill migrated beneficiaries' payout details.
-- The legacy `beneficiary` table has only name/phone/address; the actual bank and
-- mobile-money details live in `bank_deposit` / `mobile_wallet` (keyed by the old
-- BENEFICIARY_ID). PHASE 6 didn't bring them, so the admin "Beneficiary Details"
-- popup showed bank name / account / branch / swift / phone as empty.
-- Join through migration_beneficiary_map and populate the new beneficiary columns.
-- =============================================================================
USE remitm;
SET SESSION sql_mode = '';

-- ---- bank account details (latest bank_deposit row per legacy beneficiary) ----
UPDATE beneficiaries b
JOIN migration_beneficiary_map m ON m.new_beneficiary_id = b.id
JOIN (
    SELECT bd.BENEFICIARY_ID, bd.BANK_NM, bd.BRANCH_NM, bd.ACCOUNT_NUMBER,
           bd.ROUTING_NUMBER, bd.SWIFT_CD, bd.IBAN
    FROM remitm_old.bank_deposit bd
    JOIN (SELECT BENEFICIARY_ID, MAX(BANK_DEPOSIT_ID) mx
          FROM remitm_old.bank_deposit GROUP BY BENEFICIARY_ID) last
      ON last.BENEFICIARY_ID = bd.BENEFICIARY_ID AND last.mx = bd.BANK_DEPOSIT_ID
) bd ON bd.BENEFICIARY_ID = m.old_beneficiary_id
SET b.bank_name      = NULLIF(TRIM(bd.BANK_NM), ''),
    b.account_number = NULLIF(TRIM(bd.ACCOUNT_NUMBER), ''),
    b.branch_state   = NULLIF(TRIM(bd.BRANCH_NM), ''),
    b.swift_bic      = NULLIF(TRIM(bd.SWIFT_CD), ''),
    b.iban           = NULLIF(TRIM(bd.IBAN), ''),
    b.sort_code      = NULLIF(TRIM(bd.ROUTING_NUMBER), '');

-- ---- mobile-money details (latest mobile_wallet row per legacy beneficiary) ----
UPDATE beneficiaries b
JOIN migration_beneficiary_map m ON m.new_beneficiary_id = b.id
JOIN (
    SELECT mw.BENEFICIARY_ID, mw.MOBILE_CODE, mw.MOBILE_NUMBER, mw.MOBILE_SERVICE_NAME
    FROM remitm_old.mobile_wallet mw
    JOIN (SELECT BENEFICIARY_ID, MAX(MOBILE_WALLET_ID) mx
          FROM remitm_old.mobile_wallet GROUP BY BENEFICIARY_ID) last
      ON last.BENEFICIARY_ID = mw.BENEFICIARY_ID AND last.mx = mw.MOBILE_WALLET_ID
) mw ON mw.BENEFICIARY_ID = m.old_beneficiary_id
SET b.mobile_number   = NULLIF(TRIM(CONCAT(COALESCE(mw.MOBILE_CODE,''), mw.MOBILE_NUMBER)), ''),
    b.mobile_provider = NULLIF(TRIM(mw.MOBILE_SERVICE_NAME), '');

-- ---- telephone: fall back to the legacy beneficiary PHONE_NUMBER where still blank ----
UPDATE beneficiaries b
JOIN migration_beneficiary_map m ON m.new_beneficiary_id = b.id
JOIN remitm_old.beneficiary ob ON ob.BENEFICIARY_ID = m.old_beneficiary_id
SET b.mobile_number = NULLIF(TRIM(ob.PHONE_NUMBER), '')
WHERE (b.mobile_number IS NULL OR b.mobile_number = '')
  AND NULLIF(TRIM(ob.PHONE_NUMBER), '') IS NOT NULL;

-- ---- address fall back from legacy beneficiary.ADDRESS where blank ----
UPDATE beneficiaries b
JOIN migration_beneficiary_map m ON m.new_beneficiary_id = b.id
JOIN remitm_old.beneficiary ob ON ob.BENEFICIARY_ID = m.old_beneficiary_id
SET b.address = NULLIF(TRIM(ob.ADDRESS), '')
WHERE (b.address IS NULL OR b.address = '')
  AND NULLIF(TRIM(ob.ADDRESS), '') IS NOT NULL;
