package com.example.bank.transaction.client;

import java.math.BigDecimal;

public record AccountBalanceResponse(Long accountId, BigDecimal balance, String source) {
}
