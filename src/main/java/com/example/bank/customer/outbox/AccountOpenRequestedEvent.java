package com.example.bank.messaging;

import java.math.BigDecimal;

/** Durable command published after a customer has been committed locally. */
public record AccountOpenRequestedEvent(String eventId, Long customerId, String customerNo, BigDecimal initialBalance) {
}
