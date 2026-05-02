package com.sportsbetting.oddsservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportsbetting.oddsservice.model.DomainEvent;
import com.sportsbetting.oddsservice.model.OddsQuoteEntity;
import com.sportsbetting.oddsservice.model.OddsUpdate;
import com.sportsbetting.oddsservice.model.OutboxEventEntity;
import com.sportsbetting.oddsservice.repository.OddsQuoteRepository;
import com.sportsbetting.oddsservice.repository.OutboxEventRepository;
import com.sportsbetting.platform.messaging.EventPayloadValidator;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OddsService {

    private final OddsQuoteRepository oddsQuoteRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;
    private final ObjectProvider<EventPayloadValidator> eventSchemaValidator;
    private final Validator validator;
    private final int feedChunkSize;

    public OddsService(
            OddsQuoteRepository oddsQuoteRepository,
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper,
            TransactionTemplate transactionTemplate,
            ObjectProvider<EventPayloadValidator> eventSchemaValidator,
            Validator validator,
            @Value("${app.odds.feed.chunk-size:50}") int feedChunkSize
    ) {
        this.oddsQuoteRepository = oddsQuoteRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.transactionTemplate = transactionTemplate;
        this.eventSchemaValidator = eventSchemaValidator;
        this.validator = validator;
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
            final List<OddsUpdate> chunkCopy = List.copyOf(chunk);
            transactionTemplate.executeWithoutResult(status -> processChunk(chunkCopy));
        }
    }

    private void processChunk(List<OddsUpdate> chunk) {
        validateUpdates(chunk);
        List<String> keyIds = chunk.stream()
                .map(u -> key(u.eventId(), u.selection()))
                .distinct()
                .toList();
        Map<String, OddsQuoteEntity> quotesByKey = oddsQuoteRepository.findAllByKeyIdIn(keyIds).stream()
                .collect(Collectors.toMap(OddsQuoteEntity::getKeyId, Function.identity(), (a, b) -> a, LinkedHashMap::new));

        Instant batchInstant = Instant.now();
        List<OutboxEventEntity> outboxes = new ArrayList<>(chunk.size());

        for (OddsUpdate update : chunk) {
            String keyId = key(update.eventId(), update.selection());
            BigDecimal normalized = money(update.odds());
            OddsQuoteEntity quote = quotesByKey.computeIfAbsent(keyId, k -> new OddsQuoteEntity());
            quote.setKeyId(keyId);
            quote.setEventId(update.eventId());
            quote.setSelection(update.selection());
            quote.setOdds(normalized);
            quote.setUpdatedAt(batchInstant);

            Map<String, Object> payload = Map.of(
                    "eventId", update.eventId(),
                    "selection", update.selection(),
                    "odds", normalized,
                    "updatedAt", batchInstant.toString()
            );
            outboxes.add(buildOutboxEntity("odds.updated.v1", payload));
        }

        oddsQuoteRepository.saveAll(quotesByKey.values());
        outboxEventRepository.saveAll(outboxes);
    }

    private void validateUpdates(Collection<OddsUpdate> updates) {
        for (OddsUpdate update : updates) {
            Set<ConstraintViolation<OddsUpdate>> violations = validator.validate(update);
            if (!violations.isEmpty()) {
                String msg = violations.stream()
                        .map(ConstraintViolation::getMessage)
                        .collect(Collectors.joining("; "));
                throw new IllegalArgumentException(msg);
            }
        }
    }

    @Transactional(readOnly = true)
    public Optional<BigDecimal> getOdds(String eventId, String selection) {
        return oddsQuoteRepository.findByEventIdAndSelection(eventId, selection)
                .map(OddsQuoteEntity::getOdds)
                .map(this::money);
    }

    @Transactional(readOnly = true)
    public List<DomainEvent> outboxEvents() {
        List<DomainEvent> events = new ArrayList<>();
        for (OutboxEventEntity outbox : outboxEventRepository.findAll()) {
            events.add(new DomainEvent(outbox.getEventType(), parsePayload(outbox.getPayload())));
        }
        return events;
    }

    @Transactional
    public void resetForTests() {
        outboxEventRepository.deleteAll();
        oddsQuoteRepository.deleteAll();
    }

    private OutboxEventEntity buildOutboxEntity(String eventType, Map<String, Object> payload) {
        String json = writeJson(payload);
        eventSchemaValidator.ifAvailable(v -> v.validateIfPresent(eventType, json));
        OutboxEventEntity outbox = new OutboxEventEntity();
        outbox.setId(UUID.randomUUID());
        outbox.setEventType(eventType);
        outbox.setPayload(json);
        outbox.setPublished(false);
        outbox.setCreatedAt(Instant.now());
        return outbox;
    }

    private Map<String, Object> parsePayload(String payload) {
        try {
            return objectMapper.readValue(payload, LinkedHashMap.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to parse outbox payload", e);
        }
    }

    private String writeJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to write outbox payload", e);
        }
    }

    private String key(String eventId, String selection) {
        return eventId + "::" + selection;
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
