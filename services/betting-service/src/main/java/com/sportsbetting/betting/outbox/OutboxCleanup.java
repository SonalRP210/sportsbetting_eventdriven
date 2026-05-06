package com.sportsbetting.betting.outbox;

import com.sportsbetting.betting.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * Deletes old rows from {@code betting.outbox_events}. With Debezium as primary relay, rows may
 * never be marked {@code published=true}; this job prevents unbounded growth (same pattern as odds-service).
 */
@Component
public class OutboxCleanup {

    private static final Logger log = LoggerFactory.getLogger(OutboxCleanup.class);

    private final OutboxEventRepository outboxEventRepository;
    private final long retentionHours;

    public OutboxCleanup(
            OutboxEventRepository repository,
            @Value("${app.outbox.retention-hours:2}") long retentionHours
    ) {
        this.outboxEventRepository = repository;
        this.retentionHours = retentionHours;
    }

    @Scheduled(cron = "${app.outbox.cleanup.cron:0 */15 * * * *}")
    @Transactional
    public void cleanup() {
        Instant cutoff = Instant.now().minus(Duration.ofHours(retentionHours));
        int deleted = outboxEventRepository.deleteByCreatedAtBefore(cutoff);
        if (deleted > 0) {
            log.info("Outbox cleanup: deleted {} rows older than {} hours", deleted, retentionHours);
        }
    }
}
