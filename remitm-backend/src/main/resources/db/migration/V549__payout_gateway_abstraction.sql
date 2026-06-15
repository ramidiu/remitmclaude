-- Payout gateway abstraction: a payout partner now carries the GATEWAY (how it disburses),
-- the routing decision is stamped on each transaction, and gateway credentials live in a table.

-- 1) Each payout partner is associated with a gateway type (NSANO / ZEEPAY / MANUAL / ...).
--    Nullable + defaulted so existing partner-creation code keeps working; null is treated as MANUAL.
ALTER TABLE payout_partners ADD COLUMN gateway VARCHAR(32) NULL DEFAULT 'MANUAL';

-- 2) Stamp the resolved gateway on the transaction at creation (immutable routing decision,
--    so re-routing a corridor never affects in-flight transactions).
ALTER TABLE transactions ADD COLUMN payout_gateway VARCHAR(32) NULL;

-- 3) Per-gateway credentials/config so API keys/tokens rotate WITHOUT redeploy.
CREATE TABLE gateway_config (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    gateway           VARCHAR(32)  NOT NULL,
    payout_partner_id BIGINT       NULL,
    base_url          VARCHAR(255) NULL,
    api_key           VARCHAR(512) NULL,
    api_token         TEXT         NULL,
    is_active         TINYINT(1)   NOT NULL DEFAULT 1,
    token_expires_at  DATETIME     NULL,
    notes             VARCHAR(255) NULL,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_gateway_config_gateway (gateway)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4) Tag existing partners with their gateway by name (everything else stays MANUAL).
UPDATE payout_partners SET gateway = 'NSANO'
 WHERE LOWER(partner_name) LIKE '%nsano%';
UPDATE payout_partners SET gateway = 'ZEEPAY'
 WHERE LOWER(partner_name) LIKE '%zeepay%' OR LOWER(partner_name) LIKE '%zee pay%';
