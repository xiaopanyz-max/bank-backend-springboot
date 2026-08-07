package com.example.bank.account.api;

import java.math.BigDecimal;

/** Result of opening an account with server-generated identifiers. */
public record AccountCreatedResponse(Long accountId, String accountNo, BigDecimal balance) {
}
