# RemitM DB Snapshot (test data removed)

Created after removing all test transactions, customers, and the payin/payout
partner login accounts. Kept: the two seeded admin logins and all system config
(roles/permissions, countries, banks, payout_types, payout_partners routing
[NSano/ZEEPAY/MANUAL], payin_partners, corridor config, sanctions_lists, etc.).

This snapshot is committed to the repo on purpose so a fresh `git clone` comes
with a ready-to-use database and the document files (see `.gitignore` negation
rules for `db-snapshot/`). Files are gzip-compressed to stay under GitHub's
100 MB per-file limit.

## Contents
- `remitm-clean.sql.gz` — gzip of the full mysqldump of the CLEANED database
                          (schema + data). Bulk of the size is `sanctions_lists`
                          reference data.
- `kyc-uploads.tar.gz`  — the previously-uploaded KYC document files (164).
                          NOTE: their DB rows (kyc_documents) were removed with the
                          test users, so these files are now unreferenced. Included
                          so the document set is preserved with the dump.

## Restore (fresh clone)
    # DB — pipe the gzip straight into mysql
    gunzip -c db-snapshot/remitm-clean.sql.gz | docker exec -i <mysql> mysql -uroot -proot remitm
    # documents — into the backend kyc-uploads volume (mounted at /app/kyc-uploads)
    docker exec -i <backend> sh -c 'tar xzf - -C /app' < db-snapshot/kyc-uploads.tar.gz

## Remaining accounts
- admin@remitm.com         (SUPER_ADMIN)
- platformadmin@remitm.com (ADMIN)
