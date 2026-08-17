package com.example.bank.transaction.controller;

import com.example.bank.common.idempotency.annotation.ApsIdempotent;
import com.example.bank.common.result.Result;
import com.example.bank.transaction.dto.TransactionCreateDTO;
import com.example.bank.transaction.service.TransactionQueryService;
import com.example.bank.transaction.service.impl.TransactionIdempotentResultResolver;
import com.example.bank.transaction.vo.TransactionWithBalanceVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionQueryService transactionQueryService;

    public TransactionController(TransactionQueryService transactionQueryService) {
        this.transactionQueryService = transactionQueryService;
    }

    /** Creates a transaction. globalSerialNo is the upstream global idempotency key. */
    @ApsIdempotent(businessType = TransactionIdempotentResultResolver.BUSINESS_TYPE)
    @PostMapping
    public Result<TransactionWithBalanceVO> create(@Valid @RequestBody TransactionCreateDTO dto) {
        return Result.success(transactionQueryService.create(dto));
    }

    /** Returns one transaction and fetches its current balance from account-service. */
    @GetMapping("/{id}/with-balance")
    public Result<TransactionWithBalanceVO> detailWithBalance(@PathVariable Long id) {
        return Result.success(transactionQueryService.detailWithBalance(id));
    }
}
