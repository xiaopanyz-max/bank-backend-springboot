package com.example.bank.transaction.controller;

import com.example.bank.common.result.Result;
import com.example.bank.transaction.service.TransactionQueryService;
import com.example.bank.transaction.vo.TransactionWithBalanceVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionQueryService transactionQueryService;

    public TransactionController(TransactionQueryService transactionQueryService) {
        this.transactionQueryService = transactionQueryService;
    }

    /** Returns one transaction and fetches its current balance from account-service. */
    @GetMapping("/{id}/with-balance")
    public Result<TransactionWithBalanceVO> detailWithBalance(@PathVariable Long id) {
        return Result.success(transactionQueryService.detailWithBalance(id));
    }
}
