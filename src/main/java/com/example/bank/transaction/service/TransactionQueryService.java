package com.example.bank.transaction.service;

import com.example.bank.transaction.dto.TransactionCreateDTO;
import com.example.bank.transaction.vo.TransactionWithBalanceVO;

public interface TransactionQueryService {

    TransactionWithBalanceVO create(TransactionCreateDTO dto);

    TransactionWithBalanceVO detailWithBalance(Long id);
}
