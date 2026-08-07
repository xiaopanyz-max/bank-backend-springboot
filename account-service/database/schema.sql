CREATE DATABASE IF NOT EXISTS bank_account
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

USE bank_account;

CREATE TABLE IF NOT EXISTS t_account (
    id BIGINT NOT NULL AUTO_INCREMENT,
    customer_no VARCHAR(64) NOT NULL,
    account_no VARCHAR(64) NOT NULL,
    source_event_id CHAR(36) NULL,
    balance DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_account_customer_no (customer_no),
    UNIQUE KEY uk_account_no (account_no),
    UNIQUE KEY uk_account_source_event_id (source_event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
