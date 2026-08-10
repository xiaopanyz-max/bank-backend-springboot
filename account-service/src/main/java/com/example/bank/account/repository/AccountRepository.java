package com.example.bank.account.repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.UUID;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class AccountRepository {

    private final JdbcTemplate jdbcTemplate;

    public AccountRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Inserts an account with a temporary unique number and returns its database-generated ID. */
    public Long create(String customerNo, BigDecimal initialBalance) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO t_account (customer_no, account_no, source_event_id, balance) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, customerNo);
            statement.setString(2, "PENDING-" + UUID.randomUUID());
            statement.setNull(3, java.sql.Types.CHAR);
            statement.setBigDecimal(4, initialBalance);
            return statement;
        }, keyHolder);

        Number generatedId = keyHolder.getKey();
        if (generatedId == null) {
            throw new IllegalStateException("Account database did not return a generated ID");
        }
        return generatedId.longValue();
    }

    public Optional<BigDecimal> findBalance(Long accountId) {
        return jdbcTemplate.query("SELECT balance FROM t_account WHERE id = ?", (rs, rowNum) -> rs.getBigDecimal("balance"), accountId)
                .stream()
                .findFirst();
    }

    /** Uses the generated account ID to create a readable, globally unique account number. */
    public String assignAccountNo(Long accountId) {
        String accountNo = "ACC-" + String.format("%020d", accountId);
        if (jdbcTemplate.update("UPDATE t_account SET account_no = ? WHERE id = ?", accountNo, accountId) != 1) {
            throw new IllegalStateException("Newly created account was not found");
        }
        return accountNo;
    }

    public boolean updateBalance(Long accountId, BigDecimal balance) {
        return jdbcTemplate.update("UPDATE t_account SET balance = ? WHERE id = ?", balance, accountId) == 1;
    }
}
