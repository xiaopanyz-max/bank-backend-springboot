package com.example.bank.transaction.client;

import java.math.BigDecimal;

/** Account creation command. Identifiers are generated only by account-service. */
public record CreateAccountRequest(String customerNo, BigDecimal initialBalance) {
}
