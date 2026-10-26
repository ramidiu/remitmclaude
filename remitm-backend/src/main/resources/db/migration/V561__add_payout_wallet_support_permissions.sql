-- Phase 1 security hardening.
-- Adds three granular permissions used by newly-protected endpoints and grants
-- them to the full-access staff roles (ADMIN, SUPER_ADMIN). Idempotent via
-- INSERT IGNORE so re-running against a partially-seeded database is safe.
--
-- Also retires the Kuber pay-in routing toggle: the Kuber integration has been
-- removed from the codebase, so its system_config catalog row is deleted.

INSERT IGNORE INTO permissions (code, description) VALUES
    ('payout:dispatch', 'Dispatch/route payouts and mark paid from the gateway operations console'),
    ('wallet:manage',   'View and adjust customer wallet balances (admin wallet management)'),
    ('support:manage',  'View, reply to and update all customer support tickets');

INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name IN ('ADMIN', 'SUPER_ADMIN')
  AND p.code IN ('payout:dispatch', 'wallet:manage', 'support:manage');

-- Kuber pay-in rail retired — remove its system controls catalog toggle.
DELETE FROM system_config WHERE config_key = 'routing.payin.kuber.enabled';
