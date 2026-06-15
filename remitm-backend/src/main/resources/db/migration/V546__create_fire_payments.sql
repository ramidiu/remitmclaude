CREATE TABLE IF NOT EXISTS fire_payments (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_id VARCHAR(100),
    fire_code      VARCHAR(100),
    payment_uuid   VARCHAR(100),
    ican_to        VARCHAR(50),
    currency       VARCHAR(10)  DEFAULT 'GBP',
    amount         DECIMAL(18,2),
    my_reference   VARCHAR(100),
    description    VARCHAR(255),
    return_url     VARCHAR(255),
    status         VARCHAR(50)  DEFAULT 'PENDING',
    created_at     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_fire_txn_id    (transaction_id),
    INDEX idx_fire_code      (fire_code),
    INDEX idx_fire_pay_uuid  (payment_uuid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
