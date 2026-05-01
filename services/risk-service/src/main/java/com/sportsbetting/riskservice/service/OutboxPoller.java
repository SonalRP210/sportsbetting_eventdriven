package com.sportsbetting.riskservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.outbox.poll.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxPoller {
    private static final Logger log = LoggerFactory.getLogger(OutboxPoller.class);
    private final OutboxDispatcher outboxDispatcher;

    public OutboxPoller(OutboxDispatcher outboxDispatcher) { this.outboxDispatcher = outboxDispatcher; }

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:1000}")
    public void poll() {
        try { outboxDispatcher.dispatchPending(); }
        catch (Exception ex) { log.debug("Outbox dispatch will retry", ex); }
    }
}
