-- =============================================================================
-- STEP 7 — Migrate legacy payin/payout gateway data from remitm_old → remitm.
--   - zee_pay  (414) → remitm.zee_pay
--   - nsano    (90)  → remitm.nsano
--   - fire_payment_transaction (122, payin/funding) → remitm.fire_payments
--   - assign transactions.payout_gateway / payout_partner_id from the logs
--     (zee_pay→ZEEPAY/7, nsano→NSANO/6, everything else incl. CrossSwitch→MANUAL/8)
-- Clears the stale TEST rows in the new gateway tables first.
-- =============================================================================
USE remitm;
SET SESSION sql_mode = '';
SET FOREIGN_KEY_CHECKS = 0;

-- ---- 1. clear stale test data in new gateway tables ----
DELETE FROM zee_pay;
DELETE FROM nsano;
DELETE FROM fire_payments;

-- ---- 2. zee_pay (id auto) ----
INSERT INTO zee_pay
    (zee_pay_id, extra_id, transaction_id, service_type, status, last_updated, created,
     amount_charged, amount_sent, amount_pay_out, status_code, status_message,
     sender_country, sender_first_name, sender_last_name, recipient_first_name, recipient_last_name,
     created_at, updated_at)
SELECT
    ZEE_PAY_ID, EXTRA_ID, TRANSACTION_ID, SERVICE_TYPE, STATUS, LAST_UPDATED, CREATED,
    AMOUNT_CHARGED, AMOUNT_SENT, AMOUNT_PAY_OUT, STATUS_CD, STATUS_MSG,
    SENDER_COUNTRY, SENDER_FIRST_NM, SENDER_LAST_NM, RECEIPENT_FIRST_NM, RECEIPENT_LAST_NM,
    COALESCE(NULLIF(CREATED,'0000-00-00 00:00:00'), NOW()),
    COALESCE(NULLIF(LAST_UPDATED,'0000-00-00 00:00:00'), NOW())
FROM remitm_old.zee_pay;

-- ---- 3. nsano (id auto) ----
INSERT INTO nsano
    (transaction_id, nsano_transaction_id, message, code, status, api_status,
     sender_name, recipient_name, sender_account, recipient_account,
     source_currency, dest_currency, amount, rate, created_at, updated_at)
SELECT
    TRANSACTION_ID, NSANO_TRANSACTION_ID, MESSAGE, CODE, STATUS, API_STATUS,
    SENDER_NAME, RECIPIENT_NAME, SENDER_ACCOUNT, RECIPIENT_ACCOUNT,
    SOURCE_CURRENCY, DEST_CURRENCY, AMOUNT, RATE,
    COALESCE(NULLIF(CREATED_ON,'0000-00-00 00:00:00'), NOW()),
    COALESCE(NULLIF(CREATED_ON,'0000-00-00 00:00:00'), NOW())
FROM remitm_old.nsano;

-- ---- 4. fire_payments (payin/funding side; id auto) ----
INSERT INTO fire_payments
    (transaction_id, fire_code, payment_uuid, ican_to, currency, amount,
     my_reference, description, return_url, status, created_at, updated_at)
SELECT
    TRANSACTION_ID, NULLIF(TRIM(TYPE),''), PAYMENTUUID, ICANTO, CURRENCY, AMOUNT,
    MY_REFERENCE, DESCRIPTION, RETURN_URL, PAYMENT_STATUS,
    COALESCE(NULLIF(CREATED_ON,'0000-00-00 00:00:00'), NOW()),
    COALESCE(NULLIF(LAST_MODIFIED_ON,'0000-00-00 00:00:00'), NOW())
FROM remitm_old.fire_payment_transaction;

-- ---- 5. assign payout gateway/partner to transactions ----
-- default everything to MANUAL/8 (covers CrossSwitch, Eminent, Opticash, and any unlogged)
UPDATE transactions SET payout_gateway='MANUAL', payout_partner_id=8
WHERE payout_gateway IS NULL;
-- ZEEPAY where a zee_pay log exists
UPDATE transactions SET payout_gateway='ZEEPAY', payout_partner_id=7
WHERE reference_number IN (SELECT DISTINCT TRANSACTION_ID FROM remitm_old.zee_pay WHERE TRANSACTION_ID IS NOT NULL);
-- NSANO where an nsano log exists
UPDATE transactions SET payout_gateway='NSANO', payout_partner_id=6
WHERE reference_number IN (SELECT DISTINCT TRANSACTION_ID FROM remitm_old.nsano WHERE TRANSACTION_ID IS NOT NULL);

-- ---- 6. payout_reference from the gateway logs ----
UPDATE transactions t
JOIN (SELECT TRANSACTION_ID, MAX(ZEE_PAY_ID) ref FROM remitm_old.zee_pay GROUP BY TRANSACTION_ID) z
  ON z.TRANSACTION_ID = t.reference_number
SET t.payout_reference = z.ref
WHERE t.payout_gateway='ZEEPAY' AND z.ref IS NOT NULL;
UPDATE transactions t
JOIN (SELECT TRANSACTION_ID, MAX(NSANO_TRANSACTION_ID) ref FROM remitm_old.nsano GROUP BY TRANSACTION_ID) n
  ON n.TRANSACTION_ID = t.reference_number
SET t.payout_reference = n.ref
WHERE t.payout_gateway='NSANO' AND n.ref IS NOT NULL;

SET FOREIGN_KEY_CHECKS = 1;
