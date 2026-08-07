package com.example.bank.customer.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.bank.messaging.AccountOpenRequestedEvent;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Reliably relays committed outbox events to RocketMQ. Duplicate delivery is intentional and handled by the consumer. */
@Component
public class CustomerOutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(CustomerOutboxPublisher.class);
    public static final String ACCOUNT_OPEN_TOPIC = "account-open-requested";

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final RocketMQTemplate rocketMQTemplate;

    public CustomerOutboxPublisher(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper, RocketMQTemplate rocketMQTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.rocketMQTemplate = rocketMQTemplate;
    }

    @Scheduled(fixedDelayString = "${outbox.publish-delay:5000}")
    public void publishPendingEvents() {
        List<OutboxRow> rows = jdbcTemplate.query("""
                        SELECT event_id, payload FROM t_outbox_event
                        WHERE status IN ('NEW', 'RETRY') AND next_retry_time <= NOW(3)
                        ORDER BY create_time LIMIT 20
                        """,
                (rs, rowNum) -> new OutboxRow(rs.getString("event_id"), rs.getString("payload")));
        for (OutboxRow row : rows) {
            publish(row);
        }
    }

    private void publish(OutboxRow row) {
        try {
            AccountOpenRequestedEvent event = objectMapper.readValue(row.payload(), AccountOpenRequestedEvent.class);
            SendResult result = rocketMQTemplate.syncSend(ACCOUNT_OPEN_TOPIC, event);
            if (result == null || result.getSendStatus() != SendStatus.SEND_OK) {
                throw new IllegalStateException("RocketMQ did not accept event: " + result);
            }
            jdbcTemplate.update("UPDATE t_outbox_event SET status = 'PUBLISHED', published_at = NOW(3), last_error = NULL WHERE event_id = ?",
                    row.eventId());
            log.info("Published outbox event={} type=ACCOUNT_OPEN_REQUESTED", row.eventId());
        } catch (Exception ex) {
            jdbcTemplate.update("""
                    UPDATE t_outbox_event
                    SET status = 'RETRY', retry_count = retry_count + 1, last_error = ?,
                        next_retry_time = ?, update_time = NOW(3)
                    WHERE event_id = ?
                    """, truncate(ex.toString()), LocalDateTime.now().plusSeconds(10), row.eventId());
            log.warn("Could not publish outbox event={}; it will be retried", row.eventId(), ex);
        }
    }

    private String truncate(String value) {
        return value.length() <= 1000 ? value : value.substring(0, 1000);
    }

    private record OutboxRow(String eventId, String payload) {
    }
}
