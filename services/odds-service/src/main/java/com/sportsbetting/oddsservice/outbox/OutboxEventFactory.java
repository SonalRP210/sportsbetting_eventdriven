package com.sportsbetting.oddsservice.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportsbetting.oddsservice.model.outbox.OutboxEventEntity;
import com.sportsbetting.platform.messaging.EventPayloadValidator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Builds {@link OutboxEventEntity} rows from typed payloads, with JSON Schema validation when enabled.
 */
@Component
public class OutboxEventFactory {

    private final ObjectMapper objectMapper;
    private final ObjectProvider<EventPayloadValidator> eventSchemaValidator;

    public OutboxEventFactory(ObjectMapper objectMapper, ObjectProvider<EventPayloadValidator> eventSchemaValidator) {
        this.objectMapper = objectMapper;
        this.eventSchemaValidator = eventSchemaValidator;
    }

    public OutboxEventEntity createPending(String eventType, Object payloadObject) {
        String json = writeJson(payloadObject);
        eventSchemaValidator.ifAvailable(v -> v.validateIfPresent(eventType, json));
        OutboxEventEntity outbox = new OutboxEventEntity();
        outbox.setId(UUID.randomUUID());
        outbox.setEventType(eventType);
        outbox.setPayload(json);
        outbox.setPublished(false);
        outbox.setCreatedAt(Instant.now());
        return outbox;
    }

    private String writeJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to write outbox payload", e);
        }
    }
}
