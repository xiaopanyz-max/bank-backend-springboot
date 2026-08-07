package com.example.bank.messaging;

/** JSON contract for the account-open completion event. */
public record AccountOpenedEvent(String eventId, Long customerId, String customerNo, Long accountId, String accountNo) {
}
