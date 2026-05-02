package com.sportsbetting.oddsservice.service;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.outbox.poll.enabled", havingValue = "true", matchIfMissing = false)
public class OutboxPoller {
    private static final Logger log = LoggerFactory.getLogger(OutboxPoller.class);
    private final OutboxDispatcher outboxDispatcher;
    private final MeterRegistry meterRegistry;

    public OutboxPoller(OutboxDispatcher outboxDispatcher, MeterRegistry meterRegistry) {
        this.outboxDispatcher = outboxDispatcher;
        this.meterRegistry = meterRegistry;
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:1000}")
    public void poll() {
        try {
            outboxDispatcher.dispatchPending();
        } catch (Exception ex) {
            log.warn("Outbox dispatch failed; will retry on next poll", ex);
            meterRegistry.counter("outbox.dispatch.failures").increment();
        }
    }
}
