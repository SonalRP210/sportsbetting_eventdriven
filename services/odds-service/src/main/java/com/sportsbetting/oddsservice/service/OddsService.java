package com.sportsbetting.oddsservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportsbetting.oddsservice.model.DomainEvent;
import com.sportsbetting.oddsservice.model.OddsQuoteEntity;
import com.sportsbetting.oddsservice.model.OddsUpdate;
import com.sportsbetting.oddsservice.model.OutboxEventEntity;
import com.sportsbetting.oddsservice.repository.OddsQuoteRepository;
import com.sportsbetting.oddsservice.repository.OutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class OddsService {

    private final OddsQuoteRepository oddsQuoteRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OddsService(
            OddsQuoteRepository oddsQuoteRepository,
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper
    ) {
        this.oddsQuoteRepository = oddsQuoteRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void consumeOddsFeed(List<OddsUpdate> updates) {
        for (OddsUpdate update : updates) {
            if (update.eventId() == null || update.selection() == null || update.odds() == null || update.odds().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Invalid odds update");
            }
            BigDecimal normalized = money(update.odds());
            String keyId = key(update.eventId(), update.selection());
            OddsQuoteEntity quote = oddsQuoteRepository.findById(keyId).orElseGet(OddsQuoteEntity::new);
            quote.setKeyId(keyId);
            quote.setEventId(update.eventId());
            quote.setSelection(update.selection());
            quote.setOdds(normalized);
            quote.setUpdatedAt(Instant.now());
            oddsQuoteRepository.save(quote);

            persistOutbox("odds.updated.v1", Map.of(
                    "eventId", update.eventId(),
                    "selection", update.selection(),
                    "odds", normalized,
                    "updatedAt", Instant.now().toString()
            ));
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

    private void persistOutbox(String eventType, Map<String, Object> payload) {
        OutboxEventEntity outbox = new OutboxEventEntity();
        outbox.setId(UUID.randomUUID());
        outbox.setEventType(eventType);
        outbox.setPayload(writeJson(payload));
        outbox.setPublished(false);
        outbox.setCreatedAt(Instant.now());
        outboxEventRepository.save(outbox);
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
