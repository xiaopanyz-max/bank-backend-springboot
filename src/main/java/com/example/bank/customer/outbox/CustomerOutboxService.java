package com.example.bank.customer.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.bank.messaging.AccountOpenRequestedEvent;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** Persists the account-opening command in the same transaction as the customer row. */
@Service
public class CustomerOutboxService {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public CustomerOutboxService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public void recordAccountOpenRequested(Long customerId, String customerNo) {
        String eventId = UUID.randomUUID().toString();
        AccountOpenRequestedEvent event = new AccountOpenRequestedEvent(eventId, customerId, customerNo, BigDecimal.ZERO);
        try {
            jdbcTemplate.update("""
                    INSERT INTO t_outbox_event (event_id, aggregate_id, event_type, payload, status)
                    VALUES (?, ?, 'ACCOUNT_OPEN_REQUESTED', ?, 'NEW')
                    """, eventId, customerId, objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize account-open event", ex);
        }
    }
}
