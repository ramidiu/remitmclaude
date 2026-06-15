-- ============================================================================
-- V548: Zeepay payout (mobile-money / bank / cash-pickup disbursement) records.
-- ----------------------------------------------------------------------------
-- One row per Zeepay initiate attempt, updated in place as the payout is polled
-- to completion (Zeepay has no inbound callback — status is pull-only).
-- ============================================================================

CREATE TABLE IF NOT EXISTS zee_pay (
    id                    BIGINT NOT NULL AUTO_INCREMENT,
    zee_pay_id            VARCHAR(100)  DEFAULT NULL,
    extra_id              VARCHAR(100)  DEFAULT NULL,
    transaction_id        VARCHAR(100)  DEFAULT NULL,
    service_type          VARCHAR(30)   DEFAULT NULL,
    status                VARCHAR(50)   NOT NULL DEFAULT 'PENDING',
    last_updated          DATETIME      DEFAULT NULL,
    created               DATETIME      DEFAULT NULL,
    amount_charged        DECIMAL(18,4) DEFAULT NULL,
    amount_sent           DECIMAL(18,4) DEFAULT NULL,
    amount_pay_out        DECIMAL(18,4) DEFAULT NULL,
    status_code           VARCHAR(20)   DEFAULT NULL,
    status_message        TEXT          DEFAULT NULL,
    sender_country        VARCHAR(10)   DEFAULT NULL,
    sender_first_name     VARCHAR(100)  DEFAULT NULL,
    sender_last_name      VARCHAR(100)  DEFAULT NULL,
    recipient_first_name  VARCHAR(100)  DEFAULT NULL,
    recipient_last_name   VARCHAR(100)  DEFAULT NULL,
    created_at            DATETIME      NOT NULL,
    updated_at            DATETIME      NOT NULL,
    PRIMARY KEY (id),
    KEY idx_zee_pay_zee_pay_id (zee_pay_id),
    KEY idx_zee_pay_extra_id (extra_id),
    KEY idx_zee_pay_transaction_id (transaction_id),
    KEY idx_zee_pay_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
