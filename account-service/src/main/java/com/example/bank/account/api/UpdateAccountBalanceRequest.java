package com.example.bank.account.api;

import java.math.BigDecimal;

public record UpdateAccountBalanceRequest(BigDecimal balance) {
}
