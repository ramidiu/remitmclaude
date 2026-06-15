#!/bin/bash
# =============================================================================
# STEP 0 — Import old remitm1.sql into staging database (remitm_old)
#           inside the remitm-mysql Docker container.
#
# Run this ONCE before running any migration SQL.
# Usage: bash 00_setup_staging.sh
# =============================================================================

set -e

SQL_FILE="/mnt/c/Users/kreat/Downloads/remitm1 (1).sql"
CONTAINER="remitm-mysql"
DB_OLD="remitm_old"
MYSQL_USER="root"
MYSQL_PASS="root"

echo ""
echo "============================================================"
echo "  REMITM MIGRATION — Stage 0: Import Old Database"
echo "============================================================"
echo ""

# 1. Check the SQL file exists
if [ ! -f "$SQL_FILE" ]; then
    echo "ERROR: SQL file not found at: $SQL_FILE"
    exit 1
fi
echo "[OK] Source file found: $(du -sh "$SQL_FILE" | cut -f1) — $SQL_FILE"

# 2. Check container is running
if ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER}$"; then
    echo "ERROR: Container '$CONTAINER' is not running."
    echo "       Run: cd /mnt/c/Users/kreat/claude/remitmproject && docker compose -f docker-compose.remitm.yml up -d"
    exit 1
fi
echo "[OK] Container '$CONTAINER' is running"

# 3. Drop and recreate remitm_old staging database
echo ""
echo ">> Dropping existing remitm_old (if any)..."
docker exec -i "$CONTAINER" mysql -u"$MYSQL_USER" -p"$MYSQL_PASS" \
    -e "DROP DATABASE IF EXISTS remitm_old; CREATE DATABASE remitm_old CHARACTER SET latin1 COLLATE latin1_swedish_ci;" 2>/dev/null
echo "[OK] remitm_old database created"

# 4. Import the old SQL dump with strict mode disabled
echo ""
echo ">> Importing SQL dump into remitm_old (this may take a minute)..."
docker exec -i "$CONTAINER" mysql \
    -u"$MYSQL_USER" -p"$MYSQL_PASS" \
    --default-character-set=latin1 \
    -e "SET GLOBAL sql_mode=''; SET SESSION sql_mode='';" 2>/dev/null || true

# Strip the DEFINER clauses to avoid permission errors on views
sed 's/DEFINER=[^ ]* //' "$SQL_FILE" | \
docker exec -i "$CONTAINER" mysql \
    -u"$MYSQL_USER" -p"$MYSQL_PASS" \
    --default-character-set=latin1 \
    --init-command="SET SESSION sql_mode=''; SET foreign_key_checks=0;" \
    remitm_old 2>/dev/null

echo "[OK] Import complete"

# 5. Verify key tables imported
echo ""
echo ">> Verifying imported tables..."
docker exec -i "$CONTAINER" mysql -u"$MYSQL_USER" -p"$MYSQL_PASS" remitm_old 2>/dev/null <<'EOF'
SELECT
    table_name AS `Table`,
    table_rows AS `Est. Rows`
FROM information_schema.TABLES
WHERE table_schema = 'remitm_old'
  AND table_name IN ('user','user_identity','user_addr','transaction','transaction_fee',
                     'transaction_receiving_branch','beneficiary','remit_one',
                     'country','identity_type','user_type')
ORDER BY table_name;
EOF

echo ""
echo "============================================================"
echo "  Stage 0 COMPLETE."
echo "  Next: Run the infrastructure SQL:"
echo "    bash 01_run_infrastructure.sh"
echo "============================================================"
echo ""
