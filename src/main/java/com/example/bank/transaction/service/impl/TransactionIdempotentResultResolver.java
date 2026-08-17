package com.example.bank.transaction.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.bank.common.constants.ErrorCode;
import com.example.bank.common.exception.BusinessException;
import com.example.bank.common.idempotency.IdempotentResultResolver;
import com.example.bank.common.idempotency.entity.RequestRecordEntity;
import com.example.bank.transaction.client.AccountServiceClient;
import com.example.bank.transaction.entity.TransactionEntity;
import com.example.bank.transaction.mapper.TransactionMapper;
import com.example.bank.transaction.vo.TransactionWithBalanceVO;
import org.springframework.stereotype.Component;

@Component
public class TransactionIdempotentResultResolver implements IdempotentResultResolver {

    public static final String BUSINESS_TYPE = "TRANSACTION";

    private final TransactionMapper transactionMapper;
    private final AccountServiceClient accountServiceClient;

    public TransactionIdempotentResultResolver(TransactionMapper transactionMapper,
                                               AccountServiceClient accountServiceClient) {
        this.transactionMapper = transactionMapper;
        this.accountServiceClient = accountServiceClient;
    }

    @Override
    public String businessType() {
        return BUSINESS_TYPE;
    }

    @Override
    public Object resolve(RequestRecordEntity record) {
        TransactionEntity transaction = transactionMapper.selectOne(new LambdaQueryWrapper<TransactionEntity>()
                .eq(TransactionEntity::getTransactionNo, record.getGlobalSerialNo())
                .last("LIMIT 1"));
        if (transaction == null) {
            throw new BusinessException(ErrorCode.CONFLICT, "请求已处理，但交易记录不存在，请联系管理员确认流水");
        }
        var balance = accountServiceClient.getBalance(transaction.getAccountId());
        return new TransactionWithBalanceVO(
                transaction.getId(), transaction.getTransactionNo(), transaction.getAccountId(),
                transaction.getAmount(), transaction.getTransactionType(), transaction.getStatus(),
                balance.balance(), balance.source());
    }
}
