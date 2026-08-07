package com.example.bank.transaction.client;

import com.example.bank.common.exception.ServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/** Converts account-service availability failures into a controlled business response. */
@Component
public class AccountServiceClientFallbackFactory implements FallbackFactory<AccountServiceClient> {

    private static final Logger log = LoggerFactory.getLogger(AccountServiceClientFallbackFactory.class);

    @Override
    public AccountServiceClient create(Throwable cause) {
        log.warn("Account service call fell back: {}", cause.toString());
        return new AccountServiceClient() {
            @Override
            public AccountCreatedResponse createAccount(CreateAccountRequest request) {
                throw new ServiceUnavailableException("账户服务暂不可用，请稍后重试", cause);
            }

            @Override
            public AccountBalanceResponse getBalance(Long accountId) {
                throw new ServiceUnavailableException("账户服务暂不可用，请稍后重试", cause);
            }
        };
    }
}
