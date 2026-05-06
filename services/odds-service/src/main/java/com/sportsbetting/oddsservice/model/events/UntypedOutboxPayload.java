package com.sportsbetting.oddsservice.model.events;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Fallback when no dedicated Java type is registered for the stored {@code eventType}.
 */
public record UntypedOutboxPayload(JsonNode raw) implements EventPayload {

    @JsonValue
    public JsonNode raw() {
        return raw;
    }
}
