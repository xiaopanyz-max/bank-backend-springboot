package com.example.bank.account.service;

import com.example.bank.account.api.AccountBalanceResponse;
import com.example.bank.account.api.AccountCreatedResponse;
import com.example.bank.account.api.CreateAccountRequest;
import com.example.bank.account.repository.AccountRepository;
import java.math.BigDecimal;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);
    private static final String CACHE_KEY_PREFIX = "account:balance:";

    private final AccountRepository accountRepository;
    private final StringRedisTemplate redisTemplate;
    private final Duration cacheTtl;

    public AccountService(AccountRepository accountRepository,
                          StringRedisTemplate redisTemplate,
                          @Value("${account.cache-ttl}") Duration cacheTtl) {
        this.accountRepository = accountRepository;
        this.redisTemplate = redisTemplate;
        this.cacheTtl = cacheTtl;
    }

    /** Opens an account and generates both its primary key and account number server-side. */
    @Transactional
    public AccountCreatedResponse createAccount(CreateAccountRequest request) {
        if (request.customerNo() == null || request.customerNo().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer number is required");
        }
        BigDecimal initialBalance = request.initialBalance() == null ? BigDecimal.ZERO : request.initialBalance();
        if (initialBalance.signum() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Initial balance must be zero or greater");
        }
        Long accountId = accountRepository.create(request.customerNo(), initialBalance);
        String accountNo = accountRepository.assignAccountNo(accountId);
        log.info("account created customerNo={} accountId={} accountNo={} initialBalance={}",
                request.customerNo(), accountId, accountNo, initialBalance);
        return new AccountCreatedResponse(accountId, accountNo, initialBalance);
    }

    /** Reads Redis first and falls back to MySQL on a cache miss or Redis outage. */
    public AccountBalanceResponse getBalance(Long accountId) {
        String cacheKey = cacheKey(accountId);
        try {
            String cachedBalance = redisTemplate.opsForValue().get(cacheKey);
            if (cachedBalance != null) {
                log.info("account balance cache hit accountId={} cacheKey={}", accountId, cacheKey);
                return new AccountBalanceResponse(accountId, new BigDecimal(cachedBalance), "CACHE");
            }
        } catch (DataAccessException ex) {
            log.warn("Redis unavailable while reading account {}. Falling back to MySQL.", accountId);
        }

        BigDecimal balance = accountRepository.findBalance(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
        putCache(cacheKey, balance);
        log.info("account balance loaded accountId={} source=DATABASE", accountId);
        return new AccountBalanceResponse(accountId, balance, "DATABASE");
    }

    public AccountBalanceResponse updateBalance(Long accountId, BigDecimal balance) {
        if (balance == null || balance.signum() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Balance must be zero or greater");
        }
        if (!accountRepository.updateBalance(accountId, balance)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found");
        }
        putCache(cacheKey(accountId), balance);
        log.info("account balance updated accountId={} balance={}", accountId, balance);
        return new AccountBalanceResponse(accountId, balance, "DATABASE");
    }

    private void putCache(String cacheKey, BigDecimal balance) {
        try {
            redisTemplate.opsForValue().set(cacheKey, balance.toPlainString(), cacheTtl);
        } catch (DataAccessException ex) {
            log.warn("Redis unavailable while writing {}. Database remains authoritative.", cacheKey);
        }
    }

    private String cacheKey(Long accountId) {
        return CACHE_KEY_PREFIX + accountId;
    }
}
