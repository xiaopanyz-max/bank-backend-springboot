-- Convert legacy account numbers into the same server-generated format used for new accounts.
USE bank_account;

UPDATE t_account
SET account_no = CONCAT('ACC-', LPAD(CAST(id AS CHAR), 20, '0'))
WHERE account_no NOT LIKE 'ACC-%';
