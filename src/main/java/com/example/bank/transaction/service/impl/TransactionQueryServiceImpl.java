package com.example.bank.transaction.service.impl;

import com.example.bank.common.constants.ErrorCode;
import com.example.bank.common.exception.BusinessException;
import com.example.bank.transaction.client.AccountBalanceResponse;
import com.example.bank.transaction.client.AccountServiceClient;
import com.example.bank.transaction.entity.TransactionEntity;
import com.example.bank.transaction.mapper.TransactionMapper;
import com.example.bank.transaction.service.TransactionQueryService;
import com.example.bank.transaction.vo.TransactionWithBalanceVO;
import org.springframework.stereotype.Service;

@Service
public class TransactionQueryServiceImpl implements TransactionQueryService {

    private final TransactionMapper transactionMapper;
    private final AccountServiceClient accountServiceClient;

    public TransactionQueryServiceImpl(TransactionMapper transactionMapper,
                                       AccountServiceClient accountServiceClient) {
        this.transactionMapper = transactionMapper;
        this.accountServiceClient = accountServiceClient;
    }

    @Override
    public TransactionWithBalanceVO detailWithBalance(Long id) {
        TransactionEntity transaction = transactionMapper.selectById(id);
        if (transaction == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Transaction not found");
        }
        AccountBalanceResponse balance = accountServiceClient.getBalance(transaction.getAccountId());
        return toVO(transaction, balance);
    }

    TransactionWithBalanceVO toVO(TransactionEntity transaction, AccountBalanceResponse balance) {
        return new TransactionWithBalanceVO(
                transaction.getId(), transaction.getTransactionNo(), transaction.getAccountId(),
                transaction.getAmount(), transaction.getTransactionType(), transaction.getStatus(),
                balance.balance(), balance.source());
    }
}
