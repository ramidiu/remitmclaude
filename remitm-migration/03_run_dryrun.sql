-- =============================================================================
-- STEP 3 — DRY RUN: Simulate migration, show validation report, then ROLLBACK.
--           No data is permanently changed.
--
-- Execute inside container:
--   docker exec -i remitm-mysql mysql -uroot -proot remitm < 03_run_dryrun.sql
-- =============================================================================

USE remitm;
SET SESSION sql_mode = '';

SELECT '>>> Starting DRY RUN migration...' AS status;

CALL run_migration(1);
