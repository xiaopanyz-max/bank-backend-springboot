-- A customer can own multiple accounts. Account number remains globally unique.
USE bank_account;

ALTER TABLE t_account
    DROP INDEX uk_account_customer_id,
    ADD KEY idx_account_customer_id (customer_id);
