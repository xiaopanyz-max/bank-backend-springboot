package com.example.bank.account.messaging;

/** Topic names used by account-service. A consumer group replaces a RabbitMQ queue. */
public final class AccountRocketMqConfig {

    public static final String ACCOUNT_OPEN_TOPIC = "account-open-requested";
    public static final String ACCOUNT_OPENED_TOPIC = "account-opened";

    private AccountRocketMqConfig() {
    }
}
