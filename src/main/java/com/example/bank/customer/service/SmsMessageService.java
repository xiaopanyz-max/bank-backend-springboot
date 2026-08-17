package com.example.bank.customer.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.bank.customer.entity.CustomerEntity;
import com.example.bank.customer.entity.SmsMessageEntity;
import com.example.bank.transaction.client.AccountCreatedResponse;

/** Application service for SMS message lifecycle records. */
public interface SmsMessageService extends IService<SmsMessageEntity> {

    SmsMessageEntity createAccountOpenedMessage(CustomerEntity customer, AccountCreatedResponse account);

    SmsMessageEntity findByMessageId(String messageId);

    boolean shouldSkipConsumedMessage(String messageId);

    void markMqSent(String messageId);

    void markSending(String messageId);

    void markSent(String messageId);

    boolean markFailedAndShouldRetry(String messageId, String failReason);
}
