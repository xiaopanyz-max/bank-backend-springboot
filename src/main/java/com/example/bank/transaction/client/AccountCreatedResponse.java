package com.example.bank.transaction.client;

import java.math.BigDecimal;

/** Response returned when account-service opens an account. */
public record AccountCreatedResponse(Long accountId, String accountNo, BigDecimal balance) {
}
