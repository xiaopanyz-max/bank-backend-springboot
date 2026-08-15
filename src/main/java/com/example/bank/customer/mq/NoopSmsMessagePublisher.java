package com.example.bank.customer.mq;

import com.example.bank.customer.entity.SmsMessageEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Keeps local development safe when RocketMQ is not running. */
@Component
@ConditionalOnProperty(name = "bank.sms.mq-enabled", havingValue = "false", matchIfMissing = true)
public class NoopSmsMessagePublisher implements SmsMessagePublisher {

    private static final Logger log = LoggerFactory.getLogger(NoopSmsMessagePublisher.class);

    @Override
    public void publishAfterCommit(SmsMessageEntity message) {
        log.info("sms mq disabled, message recorded only messageId={} customerNo={} scene={}",
                message.getMessageId(), message.getCustomerNo(), message.getScene());
    }
}
