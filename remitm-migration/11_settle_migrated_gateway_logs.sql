-- =============================================================================
-- STEP 11 — Mark migrated gateway log records as settled so the (now-enabled)
-- status pollers don't re-poll years-old historical payouts against the live
-- Nsano/Zeepay APIs (which would error: old ids the gateway no longer knows).
--
-- Nsano poller polls records whose status NOT IN ('Paid','PAID','FAILED').
-- Migrated nsano rows carry legacy statuses ('sent','Pending') → would be polled.
-- Set them to a terminal status matching their transaction outcome.
--
-- Zeepay poller polls status IN ('PENDING','SENT_TO_PAYOUT'); migrated zee_pay
-- rows use legacy statuses ('Pending1','Success'...) so none match — no change needed.
-- =============================================================================
USE remitm;
SET SESSION sql_mode = '';

UPDATE nsano n
LEFT JOIN transactions t ON t.reference_number = n.transaction_id
SET n.status = CASE WHEN t.status IN ('CANCELLED','FAILED') THEN 'FAILED' ELSE 'Paid' END
WHERE n.status NOT IN ('Paid','PAID','FAILED');

SELECT CONCAT('Settled ', ROW_COUNT(), ' migrated nsano log records.') AS result;
