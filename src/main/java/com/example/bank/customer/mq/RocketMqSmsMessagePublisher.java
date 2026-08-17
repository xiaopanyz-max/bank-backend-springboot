package com.example.bank.customer.mq;

import com.example.bank.customer.entity.SmsMessageEntity;
import com.example.bank.customer.service.SmsMessageService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Sends SMS events to RocketMQ only after the customer transaction commits. */
@Component
@ConditionalOnProperty(name = "bank.sms.mq-enabled", havingValue = "true")
public class RocketMqSmsMessagePublisher implements SmsMessagePublisher {

    private static final Logger log = LoggerFactory.getLogger(RocketMqSmsMessagePublisher.class);

    private final RocketMQTemplate rocketMQTemplate;
    private final ObjectMapper objectMapper;
    private final SmsMessageProperties properties;
    private final SmsMessageService smsMessageService;

    public RocketMqSmsMessagePublisher(RocketMQTemplate rocketMQTemplate,
                                       ObjectMapper objectMapper,
                                       SmsMessageProperties properties,
                                       SmsMessageService smsMessageService) {
        this.rocketMQTemplate = rocketMQTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.smsMessageService = smsMessageService;
    }

    @Override
    public void publishAfterCommit(SmsMessageEntity message) {
        Runnable publishTask = () -> publish(message);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publishTask.run();
                }
            });
            return;
        }
        publishTask.run();
    }

    private void publish(SmsMessageEntity message) {
        SmsSendRequestedEvent event = new SmsSendRequestedEvent(
                message.getMessageId(),
                message.getCustomerNo(),
                message.getAccountNo(),
                message.getPhone(),
                message.getScene()
        );
        try {
            String payload = objectMapper.writeValueAsString(event);
            String destination = properties.accountOpenedDestination();
            rocketMQTemplate.syncSend(destination, payload);
            smsMessageService.markMqSent(message.getMessageId());
            log.info("sms mq message published messageId={} destination={} customerNo={} accountNo={}",
                    message.getMessageId(), destination, message.getCustomerNo(), message.getAccountNo());
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize SMS MQ event", ex);
        }
    }
}
