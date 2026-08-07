package com.example.bank.transaction.vo;

import java.math.BigDecimal;

public record TransactionWithBalanceVO(
        Long id,
        String transactionNo,
        Long accountId,
        BigDecimal amount,
        String transactionType,
        String status,
        BigDecimal currentBalance,
        String balanceSource
) {
}
