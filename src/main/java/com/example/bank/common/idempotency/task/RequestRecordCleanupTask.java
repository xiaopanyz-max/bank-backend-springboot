package com.example.bank.common.idempotency.task;

import com.example.bank.common.idempotency.service.RequestRecordService;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RequestRecordCleanupTask {

    private final RequestRecordService requestRecordService;
    private final int retentionDays;

    public RequestRecordCleanupTask(RequestRecordService requestRecordService,
                                    @Value("${bank.idempotency.retention-days:7}") int retentionDays) {
        this.requestRecordService = requestRecordService;
        this.retentionDays = retentionDays;
    }

    /** Cleans completed idempotency records every half hour. PROCESSING records are kept. */
    @Scheduled(cron = "${bank.idempotency.cleanup-cron:0 */30 * * * *}")
    public void cleanOldRecords() {
        requestRecordService.cleanCompletedBefore(LocalDateTime.now().minusDays(retentionDays));
    }
}
