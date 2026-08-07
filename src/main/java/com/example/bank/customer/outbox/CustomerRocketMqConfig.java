package com.example.bank.customer.outbox;

/** Topic names owned by customer-service. Keep them stable once messages have been produced. */
public final class CustomerRocketMqConfig {

    public static final String ACCOUNT_OPENED_TOPIC = "account-opened";

    private CustomerRocketMqConfig() {
    }
}
