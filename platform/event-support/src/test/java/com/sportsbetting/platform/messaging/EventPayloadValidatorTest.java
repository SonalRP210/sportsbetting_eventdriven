package com.sportsbetting.platform.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EventPayloadValidatorTest {

    @Test
    void acceptsValidBetPlacedPayload() {
        EventPayloadValidator validator = new EventPayloadValidator(new ObjectMapper());
        String json = """
                {"betId":"BET-1","userId":"u1","eventId":"e1","selection":"HOME","stake":10,"odds":2.5,"openRisk":25}
                """;
        assertDoesNotThrow(() -> validator.validateIfPresent("betting.bet.placed.v1", json));
    }

    @Test
    void rejectsInvalidBetPlacedPayload() {
        EventPayloadValidator validator = new EventPayloadValidator(new ObjectMapper());
        String json = "{\"betId\":\"BET-1\"}";
        assertThrows(IllegalArgumentException.class, () -> validator.validateIfPresent("betting.bet.placed.v1", json));
    }

    @Test
    void skipsUnknownEventTypes() {
        EventPayloadValidator validator = new EventPayloadValidator(new ObjectMapper());
        assertDoesNotThrow(() -> validator.validateIfPresent("unknown.topic.v1", "{}"));
    }
}
