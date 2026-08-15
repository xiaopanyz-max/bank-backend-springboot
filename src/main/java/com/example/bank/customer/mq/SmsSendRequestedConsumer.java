package com.example.bank.customer.mq;

import com.example.bank.customer.service.SmsMessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Consumes SMS send requests and simulates an SMS provider call. */
@Component
@ConditionalOnProperty(name = "bank.sms.mq-enabled", havingValue = "true")
@RocketMQMessageListener(
        topic = "${bank.sms.topic}",
        consumerGroup = "${bank.sms.consumer-group}",
        selectorExpression = "${bank.sms.account-opened-tag}"
)
public class SmsSendRequestedConsumer implements RocketMQListener<String> {

    private static final Logger log = LoggerFactory.getLogger(SmsSendRequestedConsumer.class);

    private final ObjectMapper objectMapper;
    private final SmsMessageService smsMessageService;

    public SmsSendRequestedConsumer(ObjectMapper objectMapper, SmsMessageService smsMessageService) {
        this.objectMapper = objectMapper;
        this.smsMessageService = smsMessageService;
    }

    @Override
    public void onMessage(String payload) {
        SmsSendRequestedEvent event = null;
        try {
            event = objectMapper.readValue(payload, SmsSendRequestedEvent.class);
            log.info("sms mq message consumed messageId={} customerNo={} accountNo={} scene={}",
                    event.messageId(), event.customerNo(), event.accountNo(), event.scene());
            smsMessageService.markSending(event.messageId());
            simulateSmsSend(event);
            smsMessageService.markSent(event.messageId());
            log.info("sms send mock success messageId={} phone={} scene={}",
                    event.messageId(), maskPhone(event.phone()), event.scene());
        } catch (Exception ex) {
            String messageId = event == null ? "unknown" : event.messageId();
            log.error("sms send mock failed messageId={} reason={}", messageId, ex.getMessage(), ex);
            if (event != null) {
                smsMessageService.markFailed(event.messageId(), ex.getMessage());
            }
            throw new IllegalStateException("SMS send failed, RocketMQ should retry this message", ex);
        }
    }

    private void simulateSmsSend(SmsSendRequestedEvent event) {
        log.info("sms provider mock sending messageId={} phone={} contentScene={}",
                event.messageId(), maskPhone(event.phone()), event.scene());
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return "unknown";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
