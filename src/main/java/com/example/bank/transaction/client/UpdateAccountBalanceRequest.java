package com.example.bank.transaction.client;

import java.math.BigDecimal;

public record UpdateAccountBalanceRequest(BigDecimal balance) {
}
