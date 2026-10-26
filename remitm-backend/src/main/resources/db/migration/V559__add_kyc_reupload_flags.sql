-- Address-change re-verification: when a customer changes their address, they are
-- required to re-upload proof of address + identity. This flag persists that requirement
-- so it is re-enforced on every login until the required documents are re-submitted.
ALTER TABLE users
    ADD COLUMN kyc_reupload_required BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN kyc_reupload_reason VARCHAR(100) NULL,
    ADD COLUMN kyc_reupload_requested_at DATETIME NULL;
