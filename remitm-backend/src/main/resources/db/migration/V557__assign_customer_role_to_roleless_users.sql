-- ============================================================================
-- V557: Give every role-less user the CUSTOMER role.
-- ----------------------------------------------------------------------------
-- Migrated/imported users were created without a user_roles row, so their JWT
-- carried no permissions and POST /api/transactions failed with "Access Denied"
-- (the endpoint requires 'transaction:create', granted via the CUSTOMER role).
-- New sign-ups get CUSTOMER automatically; this backfills everyone else.
-- Idempotent — only users with no role are touched. CUSTOMER is least-privilege,
-- so this cannot escalate anyone. The import procedures (remitm-migration) do the
-- same at import time; this keeps the Flyway-managed DB consistent.
-- ============================================================================

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, (SELECT id FROM roles WHERE name = 'CUSTOMER' LIMIT 1)
FROM users u
WHERE EXISTS (SELECT 1 FROM roles WHERE name = 'CUSTOMER')
  AND NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id);
