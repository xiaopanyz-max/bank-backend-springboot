-- Store the customer business number as the account ownership reference.
-- Run after V2 through V5 when upgrading the existing bank_account schema.
USE bank_account;

ALTER TABLE t_account
    ADD COLUMN customer_no VARCHAR(64) NULL AFTER id;

UPDATE t_account AS account_row
JOIN bank_dev.t_customer AS customer_row ON customer_row.id = account_row.customer_id
SET account_row.customer_no = customer_row.customer_no
WHERE account_row.customer_no IS NULL;

ALTER TABLE t_account
    MODIFY COLUMN customer_no VARCHAR(64) NOT NULL,
    DROP INDEX idx_account_customer_id,
    DROP COLUMN customer_id,
    ADD KEY idx_account_customer_no (customer_no);
