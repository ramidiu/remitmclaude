-- =============================================================================
-- STEP 4 — LIVE RUN: Execute migration and COMMIT permanently.
--           !! ONLY RUN AFTER REVIEWING THE DRY RUN REPORT !!
--
-- Execute inside container:
--   docker exec -i remitm-mysql mysql -uroot -proot remitm < 04_run_live.sql
-- =============================================================================

USE remitm;
SET SESSION sql_mode = '';

SELECT '>>> Starting LIVE migration — this will COMMIT data.' AS status;

CALL run_migration(0);
