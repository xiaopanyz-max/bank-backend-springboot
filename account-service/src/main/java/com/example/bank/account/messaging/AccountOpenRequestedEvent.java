package com.example.bank.messaging;

import java.math.BigDecimal;

/** JSON contract for the customer-service account-open command. */
public record AccountOpenRequestedEvent(String eventId, Long customerId, String customerNo, BigDecimal initialBalance) {
}
