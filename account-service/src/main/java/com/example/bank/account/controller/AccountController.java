package com.example.bank.account.controller;

import com.example.bank.account.api.AccountBalanceResponse;
import com.example.bank.account.api.AccountCreatedResponse;
import com.example.bank.account.api.CreateAccountRequest;
import com.example.bank.account.api.UpdateAccountBalanceRequest;
import com.example.bank.account.service.AccountService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountCreatedResponse createAccount(@RequestBody CreateAccountRequest request) {
        return accountService.createAccount(request);
    }

    @GetMapping("/{accountId}/balance")
    public AccountBalanceResponse getBalance(@PathVariable Long accountId) {
        return accountService.getBalance(accountId);
    }

    @PutMapping("/{accountId}/balance")
    public AccountBalanceResponse updateBalance(@PathVariable Long accountId,
                                                @RequestBody UpdateAccountBalanceRequest request) {
        return accountService.updateBalance(accountId, request.balance());
    }
}
