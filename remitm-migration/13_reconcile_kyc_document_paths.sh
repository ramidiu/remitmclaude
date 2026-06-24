#!/usr/bin/env bash
# =============================================================================
# STEP 13 — Reconcile kyc_documents.file_path to the seeded KYC image files.
#
# The legacy migration stored bare filenames (e.g. "19286_1.jpg") in
# kyc_documents.file_path, but the backend serves a document via
#   Paths.get(doc.getFilePath())   (KycController) — resolved relative to /app.
# The actual files (from db-snapshot/kyc-uploads.tar.gz) live under
#   <KYC_DIR>/<userFolder>/{address|identity}_<legacyId>_*.<ext>
# which the backend sees as /app/kyc-uploads/<userFolder>/<file>.
#
# This rewrites each document's file_path to "kyc-uploads/<userFolder>/<file>"
# by matching the legacy id embedded in the old file_path + the document type
# (PROOF_OF_ADDRESS -> address_*, everything else -> identity_*). Idempotent:
# already-pathed rows (file_path LIKE 'kyc-uploads/%') are skipped, and rows with
# no matching file on disk are left untouched.
#
# Run AFTER seeding kyc-uploads.tar.gz into KYC_DIR.
#
# Usage (server):
#   KYC_DIR=/var/www/remitm/data/kyc-uploads-live MYSQL_CONTAINER=remitm-live-mysql \
#     bash 13_reconcile_kyc_document_paths.sh
# Usage (local docker):
#   KYC_DIR=/var/lib/docker/volumes/remitmproject_remitm_kyc_uploads/_data \
#   MYSQL_CONTAINER=remitm-mysql bash 13_reconcile_kyc_document_paths.sh
# =============================================================================
set -euo pipefail

KYC_DIR="${KYC_DIR:?set KYC_DIR (host path to the kyc-uploads files)}"
MYSQL_CONTAINER="${MYSQL_CONTAINER:-remitm-mysql}"
DB="${DB:-remitm}"
UPLOAD_PREFIX="${UPLOAD_PREFIX:-kyc-uploads}"   # path prefix the backend resolves under /app
PW="${MYSQL_PASSWORD:-$(docker exec "$MYSQL_CONTAINER" sh -c 'printf %s "$MYSQL_ROOT_PASSWORD"')}"

q(){ docker exec "$MYSQL_CONTAINER" mysql -uroot -p"$PW" "$DB" -N -e "$1" 2>/dev/null; }

tmp=$(mktemp)
# candidates = bare legacy filenames not yet reconciled
q "SELECT id, document_type, file_path FROM kyc_documents WHERE file_path REGEXP '^[0-9]+_[0-9]';" > "$tmp"

updated=0; missing=0
while IFS=$'\t' read -r id dtype fpath; do
  [ -z "$id" ] && continue
  lid=$(printf '%s' "$fpath" | grep -oE '^[0-9]+')
  [ -z "$lid" ] && continue
  if [ "$dtype" = "PROOF_OF_ADDRESS" ]; then pre="address"; else pre="identity"; fi
  f=$(find "$KYC_DIR" -type f -iname "${pre}_${lid}*" 2>/dev/null | head -1)
  [ -z "$f" ] && f=$(find "$KYC_DIR" -type f -iname "${pre}*${lid}*" 2>/dev/null | head -1)
  if [ -n "$f" ]; then
    rel="${UPLOAD_PREFIX}/${f#"$KYC_DIR"/}"
    esc=$(printf '%s' "$rel" | sed "s/'/''/g")
    q "UPDATE kyc_documents SET file_path='$esc' WHERE id=$id;"
    updated=$((updated+1))
  else
    missing=$((missing+1))
  fi
done < "$tmp"
rm -f "$tmp"

echo "KYC path reconcile: updated=$updated  no-file=$missing"
echo "  docs pointing to real files: $(q "SELECT COUNT(*) FROM kyc_documents WHERE file_path LIKE '${UPLOAD_PREFIX}/%';")"
