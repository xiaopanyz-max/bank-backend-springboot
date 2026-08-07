package com.example.bank.transaction.service;

import com.example.bank.transaction.vo.TransactionWithBalanceVO;

public interface TransactionQueryService {

    TransactionWithBalanceVO detailWithBalance(Long id);
}
