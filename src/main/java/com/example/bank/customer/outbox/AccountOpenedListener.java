package com.example.bank.customer.outbox;

import com.example.bank.messaging.AccountOpenedEvent;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Idempotently advances the customer account-opening state after the account is created. */
@Component
@RocketMQMessageListener(topic = CustomerRocketMqConfig.ACCOUNT_OPENED_TOPIC, consumerGroup = "customer-service-account-opened-consumer")
public class AccountOpenedListener implements RocketMQListener<AccountOpenedEvent> {

    private static final Logger log = LoggerFactory.getLogger(AccountOpenedListener.class);
    private final JdbcTemplate jdbcTemplate;

    public AccountOpenedListener(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void onMessage(AccountOpenedEvent event) {
        jdbcTemplate.update("UPDATE t_customer SET account_open_status = 'ACTIVE' WHERE id = ?", event.customerId());
        log.info("Customer account state activated customerId={} accountId={}", event.customerId(), event.accountId());
    }
}
