-- ============================================================================
-- V552: Remove the legacy "USI Money" payout integration entirely.
-- ----------------------------------------------------------------------------
-- USI Money was decommissioned during the RemitM rebrand (backend module,
-- superadmin page, service, routes and config were deleted). This migration
-- removes what was left behind:
--   * the 4 USI-specific tables created by V532/V533, and
--   * any orphaned "USI Money" payout_partner row + corridor mappings
--     (V545 only deactivated these; here we delete them outright).
-- Cash-pickup / collection-point functionality is unaffected — it now reads
-- the generic cash_collection_points table, not USI.
-- ============================================================================

-- 1) Delete any leftover corridor mappings pointing at the USI partner.
DELETE FROM corridor_partner_mappings
WHERE partner_id IN (SELECT id FROM payout_partners WHERE partner_name = 'USI Money');

-- 2) Delete the orphaned payout partner row.
DELETE FROM payout_partners WHERE partner_name = 'USI Money';

-- 3) Drop the USI integration tables (created by V532/V533, no live readers).
DROP TABLE IF EXISTS usi_transactions;
DROP TABLE IF EXISTS usi_beneficiaries;
DROP TABLE IF EXISTS usi_remitters;
DROP TABLE IF EXISTS usi_test_accounts;
