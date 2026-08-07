package com.example.bank.account.messaging;

import com.example.bank.account.service.AccountService;
import com.example.bank.messaging.AccountOpenRequestedEvent;
import com.example.bank.messaging.AccountOpenedEvent;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.client.producer.SendStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** RocketMQ retries this command when the account transaction or status event publication fails. */
@Component
@RocketMQMessageListener(topic = AccountRocketMqConfig.ACCOUNT_OPEN_TOPIC, consumerGroup = "account-service-account-open-consumer")
public class AccountOpenRequestedListener implements RocketMQListener<AccountOpenRequestedEvent> {

    private static final Logger log = LoggerFactory.getLogger(AccountOpenRequestedListener.class);

    private final AccountService accountService;
    private final RocketMQTemplate rocketMQTemplate;

    public AccountOpenRequestedListener(AccountService accountService, RocketMQTemplate rocketMQTemplate) {
        this.accountService = accountService;
        this.rocketMQTemplate = rocketMQTemplate;
    }

    @Override
    public void onMessage(AccountOpenRequestedEvent event) {
        var account = accountService.createAccountFromEvent(event.eventId(), event.customerNo(), event.initialBalance());
        var sendResult = rocketMQTemplate.syncSend(AccountRocketMqConfig.ACCOUNT_OPENED_TOPIC,
                new AccountOpenedEvent(event.eventId(), event.customerId(), event.customerNo(), account.accountId(), account.accountNo()));
        if (sendResult == null || sendResult.getSendStatus() != SendStatus.SEND_OK) {
            throw new IllegalStateException("RocketMQ did not accept account-opened event: " + sendResult);
        }
        log.info("Consumed account-open event={} customerNo={} accountId={}",
                event.eventId(), event.customerNo(), account.accountId());
    }
}
