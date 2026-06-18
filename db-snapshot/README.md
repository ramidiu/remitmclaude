# RemitM DB Snapshot (legacy data migrated)

Test data was removed, then the legacy production database (`remitm1.sql`, old
MariaDB system) was migrated into the new schema via `remitm-migration/`.
Contains: 2 seeded admin logins + 210 migrated customers (passwords BCrypt-hashed,
original passwords still work), 428 transactions, 243 beneficiaries, 210 wallets,
386 KYC documents, 189 KYC verifications (35 users at TIER_2), and all system
config (payin_partners, payout_partners routing [NSano/ZEEPAY/MANUAL], corridors,
sanctions_lists, etc.).

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
