-- Repairs rows created by the legacy service while customer_id was being introduced.
-- Legacy account IDs were equal to their owning customer IDs.
USE bank_account;

UPDATE t_account
SET customer_id = id
WHERE customer_id = 0;
