-- =============================================================================
-- STEP 9 — Correct migrated transactions to faithfully match the legacy data.
--   1. Remove non-legacy (test) transactions — keep ONLY legacy data.
--   2. Re-map status from the real legacy STATUS (the original PHASE 7 mapping let
--      'sent for pay'/'Paid1'/'sent'/'Rejected' fall through to PENDING, which the
--      archive scheduler then hid → customers saw far fewer transactions).
--   3. Re-assign payout_gateway/partner from each transaction's real legacy
--      API_NAME (was wrongly derived from gateway logs and folded into MANUAL).
-- =============================================================================
USE remitm;
SET SESSION sql_mode = '';
SET FOREIGN_KEY_CHECKS = 0;

-- 1. drop non-legacy test transactions + their children
DELETE le FROM ledger_entries le JOIN transactions t ON t.id=le.transaction_id
  WHERE NOT EXISTS (SELECT 1 FROM remitm_old.transaction o WHERE o.TRANSACTION_ID=t.reference_number);
DELETE p FROM payments p JOIN transactions t ON t.id=p.transaction_id
  WHERE NOT EXISTS (SELECT 1 FROM remitm_old.transaction o WHERE o.TRANSACTION_ID=t.reference_number);
DELETE h FROM transaction_status_history h JOIN transactions t ON t.id=h.transaction_id
  WHERE NOT EXISTS (SELECT 1 FROM remitm_old.transaction o WHERE o.TRANSACTION_ID=t.reference_number);
DELETE t FROM transactions t
  WHERE NOT EXISTS (SELECT 1 FROM remitm_old.transaction o WHERE o.TRANSACTION_ID=t.reference_number);

-- 2. status straight from legacy STATUS
UPDATE transactions t JOIN remitm_old.transaction o ON o.TRANSACTION_ID = t.reference_number
SET t.status = CASE LOWER(TRIM(COALESCE(o.STATUS,'')))
    WHEN 'paid'          THEN 'PAID'
    WHEN 'paid1'         THEN 'PAID'
    WHEN 'sent for pay'  THEN 'PAID'   -- dispatched to payout = completed (historical)
    WHEN 'sent'          THEN 'PAID'
    WHEN 'cancelled'     THEN 'CANCELLED'
    WHEN 'failed'        THEN 'FAILED'
    WHEN 'rejected'      THEN 'FAILED'
    WHEN 'pending'       THEN 'PENDING'
    ELSE 'PENDING'
END;

-- 3. gateway/partner straight from legacy API_NAME
UPDATE transactions t JOIN remitm_old.transaction o ON o.TRANSACTION_ID = t.reference_number
SET t.payout_gateway = CASE UPPER(TRIM(COALESCE(o.API_NAME,'')))
        WHEN 'ZEEPAY' THEN 'ZEEPAY' WHEN 'NSANO' THEN 'NSANO'
        WHEN 'EMINENT' THEN 'EMINENT' WHEN 'CROSSSWITCH' THEN 'CROSSSWITCH'
        WHEN 'OPTICASH' THEN 'OPTICASH' WHEN 'MANUAL' THEN 'MANUAL'
        ELSE 'MANUAL' END,
    t.payout_partner_id = CASE UPPER(TRIM(COALESCE(o.API_NAME,'')))
        WHEN 'ZEEPAY' THEN 7 WHEN 'NSANO' THEN 6
        WHEN 'EMINENT' THEN 9 WHEN 'CROSSSWITCH' THEN 10
        WHEN 'OPTICASH' THEN 11 ELSE 8 END;

-- 4. restore real completion/activity dates from legacy.
--    The status/gateway UPDATEs above bump updated_at to NOW() (ON UPDATE CURRENT_TIMESTAMP);
--    the partner "Completed" page shows updated_at, so set it back to the legacy
--    LAST_MODIFIED_ON (the real completion date), never earlier than created_at.
--    Must run LAST so its explicit updated_at value overrides the auto-bump.
UPDATE transactions t JOIN remitm_old.transaction o ON o.TRANSACTION_ID = t.reference_number
SET t.updated_at = GREATEST(t.created_at,
        COALESCE(NULLIF(o.LAST_MODIFIED_ON, '0000-00-00 00:00:00'), t.created_at)),
    t.payout_confirmed_at = CASE WHEN t.status IN ('PAID','COMPLETED')
        THEN GREATEST(t.created_at,
             COALESCE(NULLIF(o.LAST_MODIFIED_ON, '0000-00-00 00:00:00'), t.created_at))
        ELSE t.payout_confirmed_at END;

SET FOREIGN_KEY_CHECKS = 1;
