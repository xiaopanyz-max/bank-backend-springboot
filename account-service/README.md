# Account service

An independent account service. MySQL is the source of truth; Redis is a five-minute read-through cache.

1. Run `database/schema.sql` against MySQL.
2. Start Redis at `127.0.0.1:6379` (optional for local development; reads fall back to MySQL if it is unavailable).
3. Set `MYSQL_PASSWORD`, then run `mvn spring-boot:run` from this directory.

The customer service calls `POST http://localhost:8081/internal/accounts` after creating a customer. The transaction service calls `GET http://localhost:8081/internal/accounts/{accountId}/balance` when returning transaction details.
