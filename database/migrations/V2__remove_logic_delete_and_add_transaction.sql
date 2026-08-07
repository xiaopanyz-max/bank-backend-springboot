USE bank_dev;

-- Run this only for an existing bank_dev database created with the old schema.
ALTER TABLE t_customer DROP COLUMN IF EXISTS deleted;

CREATE TABLE IF NOT EXISTS t_transaction (
    id BIGINT NOT NULL,
    transaction_no VARCHAR(64) NOT NULL,
    account_id BIGINT NOT NULL,
    amount DECIMAL(19,4) NOT NULL,
    transaction_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    create_time DATETIME(3) NULL,
    update_time DATETIME(3) NULL,
    creator BIGINT NULL,
    updater BIGINT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_transaction_no (transaction_no),
    KEY idx_transaction_account_id (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT IGNORE INTO t_transaction (
    id, transaction_no, account_id, amount, transaction_type, status, create_time, update_time, creator, updater
) VALUES (
    10001, 'TXN-000001', 1, 100.0000, 'DEPOSIT', 'SUCCESS', NOW(3), NOW(3), 0, 0
);
