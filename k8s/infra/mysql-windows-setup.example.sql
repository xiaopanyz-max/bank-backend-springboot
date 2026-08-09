-- Run this script in a Windows-local MySQL client as an administrator account.
-- Replace the password below before executing. Do not commit the real password.

CREATE DATABASE IF NOT EXISTS bank_dev
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

CREATE DATABASE IF NOT EXISTS bank_account
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

-- The VMware NAT subnet is intentionally the only permitted source.
CREATE USER IF NOT EXISTS 'bank_k8s'@'192.168.30.%'
  IDENTIFIED BY 'REPLACE_WITH_A_STRONG_PASSWORD';

GRANT ALL PRIVILEGES ON bank_dev.* TO 'bank_k8s'@'192.168.30.%';
GRANT ALL PRIVILEGES ON bank_account.* TO 'bank_k8s'@'192.168.30.%';
FLUSH PRIVILEGES;

-- Then execute the existing schema files once, using a local administrator account:
--   database/schema.sql
--   account-service/database/schema.sql
