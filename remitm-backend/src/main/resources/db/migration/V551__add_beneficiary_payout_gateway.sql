-- Per-beneficiary payout rail (carried from the old site's API_NAME). Lets the recipient picker
-- filter beneficiaries by the gateway/API type, not just country + delivery method.
-- NULL = compatible with any rail (app-created recipients, or legacy/unknown).
ALTER TABLE beneficiaries ADD COLUMN payout_gateway VARCHAR(32) NULL;
