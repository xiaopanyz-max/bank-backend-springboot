package com.example.bank.messaging;

/** Event emitted after account-service has durably opened an account. */
public record AccountOpenedEvent(String eventId, Long customerId, String customerNo, Long accountId, String accountNo) {
}
