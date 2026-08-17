package com.example.bank.customer.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.bank.customer.entity.CustomerEntity;
import com.example.bank.customer.entity.SmsMessageEntity;
import com.example.bank.customer.mapper.SmsMessageMapper;
import com.example.bank.customer.mq.SmsMessageProperties;
import com.example.bank.customer.mq.SmsMessageStatus;
import com.example.bank.customer.service.SmsMessageService;
import com.example.bank.transaction.client.AccountCreatedResponse;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Maintains SMS message lifecycle in t_sms_message. */
@Service
public class SmsMessageServiceImpl extends ServiceImpl<SmsMessageMapper, SmsMessageEntity>
        implements SmsMessageService {

    public static final String SCENE_ACCOUNT_OPENED = "ACCOUNT_OPENED";

    private final SmsMessageProperties properties;

    public SmsMessageServiceImpl(SmsMessageProperties properties) {
        this.properties = properties;
    }

    @Override
    public SmsMessageEntity createAccountOpenedMessage(CustomerEntity customer, AccountCreatedResponse account) {
        SmsMessageEntity message = new SmsMessageEntity();
        message.setMessageId(UUID.randomUUID().toString());
        message.setCustomerNo(customer.getCustomerNo());
        message.setAccountNo(account.accountNo());
        message.setPhone(customer.getPhone());
        message.setScene(SCENE_ACCOUNT_OPENED);
        message.setContent("您的银行账户已开户成功，账户号：" + account.accountNo());
        message.setStatus(SmsMessageStatus.INIT);
        message.setRetryCount(0);
        save(message);
        return message;
    }

    @Override
    public SmsMessageEntity findByMessageId(String messageId) {
        return lambdaQuery()
                .eq(SmsMessageEntity::getMessageId, messageId)
                .one();
    }

    @Override
    public boolean shouldSkipConsumedMessage(String messageId) {
        SmsMessageEntity message = findByMessageId(messageId);
        return message == null
                || SmsMessageStatus.SENT.equals(message.getStatus())
                || SmsMessageStatus.FAILED_FINAL.equals(message.getStatus());
    }

    @Override
    public void markMqSent(String messageId) {
        SmsMessageEntity message = findByMessageId(messageId);
        if (message == null || SmsMessageStatus.SENT.equals(message.getStatus())) {
            return;
        }
        message.setStatus(SmsMessageStatus.MQ_SENT);
        message.setFailReason(null);
        updateById(message);
    }

    @Override
    public void markSending(String messageId) {
        SmsMessageEntity message = findByMessageId(messageId);
        if (message == null
                || SmsMessageStatus.SENT.equals(message.getStatus())
                || SmsMessageStatus.FAILED_FINAL.equals(message.getStatus())) {
            return;
        }
        message.setStatus(SmsMessageStatus.SENDING);
        message.setRetryCount((message.getRetryCount() == null ? 0 : message.getRetryCount()) + 1);
        message.setFailReason(null);
        updateById(message);
    }

    @Override
    public void markSent(String messageId) {
        SmsMessageEntity message = findByMessageId(messageId);
        if (message == null) {
            return;
        }
        message.setStatus(SmsMessageStatus.SENT);
        message.setFailReason(null);
        message.setSentAt(LocalDateTime.now());
        updateById(message);
    }

    @Override
    public boolean markFailedAndShouldRetry(String messageId, String failReason) {
        SmsMessageEntity message = findByMessageId(messageId);
        if (message == null) {
            return false;
        }
        int retryCount = message.getRetryCount() == null ? 0 : message.getRetryCount();
        boolean shouldRetry = retryCount < properties.getMaxRetryCount();
        message.setStatus(shouldRetry ? SmsMessageStatus.FAILED : SmsMessageStatus.FAILED_FINAL);
        message.setFailReason(failReason == null ? null : failReason.substring(0, Math.min(failReason.length(), 500)));
        updateById(message);
        return shouldRetry;
    }
}
