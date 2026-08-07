-- Run once when upgrading an existing bank_account database.
-- Legacy rows used the customer ID as their account ID, so preserve that relationship.
USE bank_account;

ALTER TABLE t_account
    ADD COLUMN customer_id BIGINT NULL AFTER id;

UPDATE t_account
SET customer_id = id
WHERE customer_id IS NULL OR customer_id = 0;

ALTER TABLE t_account
    MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT,
    MODIFY COLUMN customer_id BIGINT NOT NULL,
    ADD UNIQUE KEY uk_account_customer_id (customer_id);
