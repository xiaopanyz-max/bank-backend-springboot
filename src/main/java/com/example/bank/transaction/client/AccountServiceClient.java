package com.example.bank.transaction.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/** Declarative remote client for account-service. */
@FeignClient(name = "account-service", path = "/internal/accounts",
        fallbackFactory = AccountServiceClientFallbackFactory.class)
public interface AccountServiceClient {

    @PostMapping
    AccountCreatedResponse createAccount(@RequestBody CreateAccountRequest request);

    @GetMapping("/{accountId}/balance")
    AccountBalanceResponse getBalance(@PathVariable("accountId") Long accountId);
}
