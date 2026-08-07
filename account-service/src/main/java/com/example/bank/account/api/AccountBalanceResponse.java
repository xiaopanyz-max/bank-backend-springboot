package com.example.bank.account.api;

import java.math.BigDecimal;

public record AccountBalanceResponse(Long accountId, BigDecimal balance, String source) {
}
