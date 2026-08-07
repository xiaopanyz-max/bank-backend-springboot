CREATE DATABASE IF NOT EXISTS bank_dev
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

USE bank_dev;

CREATE TABLE IF NOT EXISTS t_customer (
    id BIGINT NOT NULL,
    customer_no VARCHAR(64) NOT NULL,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(255) NULL,
    status INT NOT NULL DEFAULT 1,
    account_open_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    create_time DATETIME(3) NULL,
    update_time DATETIME(3) NULL,
    creator BIGINT NULL,
    updater BIGINT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_customer_customer_no (customer_no),
    KEY idx_customer_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS t_outbox_event (
    event_id CHAR(36) NOT NULL,
    aggregate_id BIGINT NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'NEW',
    retry_count INT NOT NULL DEFAULT 0,
    next_retry_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    last_error VARCHAR(1000) NULL,
    published_at DATETIME(3) NULL,
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (event_id),
    KEY idx_outbox_pending (status, next_retry_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

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
