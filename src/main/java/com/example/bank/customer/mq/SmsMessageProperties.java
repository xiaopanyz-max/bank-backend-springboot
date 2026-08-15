package com.example.bank.customer.mq;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration for customer-service SMS MQ integration. */
@ConfigurationProperties(prefix = "bank.sms")
public class SmsMessageProperties {

    private boolean mqEnabled;
    private String topic = "bank-sms-topic";
    private String accountOpenedTag = "ACCOUNT_OPENED";
    private String consumerGroup = "customer-service-sms-consumer";

    public boolean isMqEnabled() {
        return mqEnabled;
    }

    public void setMqEnabled(boolean mqEnabled) {
        this.mqEnabled = mqEnabled;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getAccountOpenedTag() {
        return accountOpenedTag;
    }

    public void setAccountOpenedTag(String accountOpenedTag) {
        this.accountOpenedTag = accountOpenedTag;
    }

    public String getConsumerGroup() {
        return consumerGroup;
    }

    public void setConsumerGroup(String consumerGroup) {
        this.consumerGroup = consumerGroup;
    }

    public String accountOpenedDestination() {
        return topic + ":" + accountOpenedTag;
    }
}
