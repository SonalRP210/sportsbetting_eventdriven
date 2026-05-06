package com.sportsbetting.oddsservice.model.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportsbetting.oddsservice.model.odds.OddsUpdatedEvent;

/**
 * Strongly typed deserialized body for rows read from {@code outbox_events}.
 * Unknown {@code eventType} strings fall back to {@link UntypedOutboxPayload}.
 */
public interface EventPayload {

    static EventPayload fromJson(ObjectMapper mapper, String eventType, String json) {
        try {
            if (OddsUpdatedEvent.EVENT_TYPE.equals(eventType)) {
                return mapper.readValue(json, OddsUpdatedEvent.class);
            }
            JsonNode node = mapper.readTree(json);
            return new UntypedOutboxPayload(node);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to parse outbox payload for event type " + eventType, e);
        }
    }
}
