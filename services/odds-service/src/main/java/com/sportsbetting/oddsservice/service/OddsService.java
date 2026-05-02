package com.sportsbetting.oddsservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportsbetting.oddsservice.ingest.OddsFeedChunkProcessor;
import com.sportsbetting.oddsservice.ingest.OddsMoney;
import com.sportsbetting.oddsservice.model.events.EventPayload;
import com.sportsbetting.oddsservice.model.odds.OddsQuoteEntity;
import com.sportsbetting.oddsservice.model.odds.OddsUpdate;
import com.sportsbetting.oddsservice.model.outbox.DomainEvent;
import com.sportsbetting.oddsservice.model.outbox.OutboxEventEntity;
import com.sportsbetting.oddsservice.repository.OddsQuoteRepository;
import com.sportsbetting.oddsservice.repository.OutboxEventRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class OddsService {

    private final OddsQuoteRepository oddsQuoteRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;
    private final OddsFeedChunkProcessor oddsFeedChunkProcessor;
    private final int feedChunkSize;

    public OddsService(
            OddsQuoteRepository oddsQuoteRepository,
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper,
            TransactionTemplate transactionTemplate,
            OddsFeedChunkProcessor oddsFeedChunkProcessor,
            @Value("${app.odds.feed.chunk-size:50}") int feedChunkSize
    ) {
        this.oddsQuoteRepository = oddsQuoteRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.transactionTemplate = transactionTemplate;
        this.oddsFeedChunkProcessor = oddsFeedChunkProcessor;
        this.feedChunkSize = Math.max(1, feedChunkSize);
    }

    /**
     * Persists odds updates in bounded transactions ({@code app.odds.feed.chunk-size} rows per commit).
     * Each chunk commits independently to limit lock duration and tail latency under large batches.
     * <p>
     * HTTP {@code 202 ACCEPTED} means each chunk attempted after Bean Validation succeeded at the API boundary;
     * if a later chunk fails after earlier chunks committed, upstream replay semantics apply — define whether that
     * is acceptable for your feed provider (same as multi-instance last-write-wins on overlapping keys).
     */
    public void consumeOddsFeed(List<OddsUpdate> updates) {
        if (updates.isEmpty()) {
            return;
        }
        for (int i = 0; i < updates.size(); i += feedChunkSize) {
            int end = Math.min(i + feedChunkSize, updates.size());
            List<OddsUpdate> chunk = updates.subList(i, end);
            List<OddsUpdate> chunkCopy = List.copyOf(chunk);
            transactionTemplate.executeWithoutResult(status -> oddsFeedChunkProcessor.process(chunkCopy));
        }
    }

    @Transactional(readOnly = true)
    public Optional<BigDecimal> getOdds(String eventId, String selection) {
        return oddsQuoteRepository.findByEventIdAndSelection(eventId, selection)
                .map(OddsQuoteEntity::getOdds)
                .map(OddsMoney::normalize);
    }

    @Transactional(readOnly = true)
    public List<DomainEvent> outboxEvents() {
        List<DomainEvent> events = new ArrayList<>();
        for (OutboxEventEntity outbox : outboxEventRepository.findAll()) {
            EventPayload payload = EventPayload.fromJson(objectMapper, outbox.getEventType(), outbox.getPayload());
            events.add(new DomainEvent(outbox.getEventType(), payload));
        }
        return events;
    }
}
