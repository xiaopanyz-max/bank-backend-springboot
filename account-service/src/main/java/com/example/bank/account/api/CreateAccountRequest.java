package com.example.bank.account.api;

import java.math.BigDecimal;

/**
 * The caller supplies the owning customer number, never an account primary key or account number.
 * The account service generates both values when persisting the row.
 */
public record CreateAccountRequest(String customerNo, BigDecimal initialBalance) {
}
