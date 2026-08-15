package com.example.bank.customer.mq;

/** MQ payload for requesting an SMS send. */
public record SmsSendRequestedEvent(
        String messageId,
        String customerNo,
        String accountNo,
        String phone,
        String scene
) {
}
