package com.example.bank.customer.mq;

import com.example.bank.customer.entity.SmsMessageEntity;

/** Publishes SMS message events after the customer transaction commits. */
public interface SmsMessagePublisher {

    void publishAfterCommit(SmsMessageEntity message);
}
