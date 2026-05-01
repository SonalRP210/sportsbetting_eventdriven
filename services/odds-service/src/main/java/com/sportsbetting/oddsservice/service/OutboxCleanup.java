package com.sportsbetting.oddsservice.service;

import com.sportsbetting.oddsservice.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * Deletes old rows from odds.outbox_events on a schedule.
 *
 * When Debezium CDC is the primary relay, rows are never marked published=true
 * and never deleted by the OutboxDispatcher. Without this cleanup the table
 * grows unbounded.
 *
 * Debezium reads INSERT events directly from the Postgres WAL — the row is
 * captured the moment it is committed, before this job even runs. It is
 * therefore safe to delete any row that is older than the retention window.
 *
 * Default retention: 2 hours (configurable via app.outbox.retention-hours).
 * Default schedule:  every 15 minutes.
 */
@Component
public class OutboxCleanup {

    private static final Logger log = LoggerFactory.getLogger(OutboxCleanup.class);

    private final OutboxEventRepository repository;
    private final long retentionHours;

    public OutboxCleanup(
            OutboxEventRepository repository,
            @Value("${app.outbox.retention-hours:2}") long retentionHours
    ) {
        this.repository = repository;
        this.retentionHours = retentionHours;
    }

    @Scheduled(cron = "${app.outbox.cleanup.cron:0 */15 * * * *}")
    @Transactional
    public void cleanup() {
        Instant cutoff = Instant.now().minus(Duration.ofHours(retentionHours));
        int deleted = repository.deleteByCreatedAtBefore(cutoff);
        if (deleted > 0) {
            log.info("Outbox cleanup: deleted {} rows older than {} hours", deleted, retentionHours);
        }
    }
}
