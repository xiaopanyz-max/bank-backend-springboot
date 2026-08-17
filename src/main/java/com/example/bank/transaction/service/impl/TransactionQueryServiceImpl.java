package com.example.bank.transaction.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.bank.common.constants.ErrorCode;
import com.example.bank.common.exception.BusinessException;
import com.example.bank.transaction.client.AccountBalanceResponse;
import com.example.bank.transaction.client.AccountServiceClient;
import com.example.bank.transaction.client.UpdateAccountBalanceRequest;
import com.example.bank.transaction.dto.TransactionCreateDTO;
import com.example.bank.transaction.entity.TransactionEntity;
import com.example.bank.transaction.mapper.TransactionMapper;
import com.example.bank.transaction.service.TransactionQueryService;
import com.example.bank.transaction.vo.TransactionWithBalanceVO;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class TransactionQueryServiceImpl implements TransactionQueryService {

    private static final Logger log = LoggerFactory.getLogger(TransactionQueryServiceImpl.class);
    private static final String TYPE_DEPOSIT = "DEPOSIT";
    private static final String TYPE_WITHDRAW = "WITHDRAW";
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";

    private final TransactionMapper transactionMapper;
    private final AccountServiceClient accountServiceClient;

    public TransactionQueryServiceImpl(TransactionMapper transactionMapper,
                                       AccountServiceClient accountServiceClient) {
        this.transactionMapper = transactionMapper;
        this.accountServiceClient = accountServiceClient;
    }

    @Override
    public TransactionWithBalanceVO create(TransactionCreateDTO dto) {
        TransactionEntity transaction = new TransactionEntity();
        transaction.setTransactionNo(dto.getGlobalSerialNo());
        transaction.setAccountId(dto.getAccountId());
        transaction.setAmount(dto.getAmount());
        transaction.setTransactionType(dto.getTransactionType());
        transaction.setStatus(STATUS_PROCESSING);

        try {
            transactionMapper.insert(transaction);
        } catch (DuplicateKeyException ex) {
            TransactionEntity existing = findByTransactionNo(dto.getGlobalSerialNo());
            if (existing == null) {
                throw ex;
            }
            assertSameTransaction(dto, existing);
            return toVO(existing, accountServiceClient.getBalance(existing.getAccountId()));
        }

        try {
            AccountBalanceResponse current = accountServiceClient.getBalance(dto.getAccountId());
            BigDecimal newBalance = calculateNewBalance(current.balance(), dto);
            AccountBalanceResponse updated = accountServiceClient.updateBalance(
                    dto.getAccountId(), new UpdateAccountBalanceRequest(newBalance));
            transaction.setStatus(STATUS_SUCCESS);
            transactionMapper.updateById(transaction);
            log.info("transaction created transactionNo={} accountId={} type={} amount={} balance={}",
                    transaction.getTransactionNo(), transaction.getAccountId(),
                    transaction.getTransactionType(), transaction.getAmount(), updated.balance());
            return toVO(transaction, updated);
        } catch (RuntimeException ex) {
            transaction.setStatus(STATUS_FAILED);
            transactionMapper.updateById(transaction);
            throw ex;
        }
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

    TransactionEntity findByTransactionNo(String transactionNo) {
        return transactionMapper.selectOne(new LambdaQueryWrapper<TransactionEntity>()
                .eq(TransactionEntity::getTransactionNo, transactionNo)
                .last("LIMIT 1"));
    }

    TransactionWithBalanceVO toVO(TransactionEntity transaction, AccountBalanceResponse balance) {
        return new TransactionWithBalanceVO(
                transaction.getId(), transaction.getTransactionNo(), transaction.getAccountId(),
                transaction.getAmount(), transaction.getTransactionType(), transaction.getStatus(),
                balance.balance(), balance.source());
    }

    private BigDecimal calculateNewBalance(BigDecimal currentBalance, TransactionCreateDTO dto) {
        if (TYPE_DEPOSIT.equals(dto.getTransactionType())) {
            return currentBalance.add(dto.getAmount());
        }
        if (TYPE_WITHDRAW.equals(dto.getTransactionType())) {
            BigDecimal newBalance = currentBalance.subtract(dto.getAmount());
            if (newBalance.signum() < 0) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "账户余额不足");
            }
            return newBalance;
        }
        throw new BusinessException(ErrorCode.BAD_REQUEST, "Unsupported transaction type: " + dto.getTransactionType());
    }

    private void assertSameTransaction(TransactionCreateDTO dto, TransactionEntity existing) {
        boolean samePayload = existing.getAccountId().equals(dto.getAccountId())
                && existing.getAmount().compareTo(dto.getAmount()) == 0
                && existing.getTransactionType().equals(dto.getTransactionType());
        if (!samePayload) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "Global serial number already exists with different transaction content: " + dto.getGlobalSerialNo());
        }
    }
}
