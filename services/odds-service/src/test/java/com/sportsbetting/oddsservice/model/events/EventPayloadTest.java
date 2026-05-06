package com.sportsbetting.oddsservice.model.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sportsbetting.oddsservice.model.odds.OddsUpdatedEvent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class EventPayloadTest {

    private final ObjectMapper mapper = createMapper();

    private static ObjectMapper createMapper() {
        ObjectMapper m = new ObjectMapper();
        m.registerModule(new JavaTimeModule());
        m.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return m;
    }

    @Test
    void fromJsonMapsOddsUpdatedTypeToRecord() throws Exception {
        Instant t = Instant.parse("2026-05-02T12:00:00Z");
        OddsUpdatedEvent original = new OddsUpdatedEvent("e1", "HOME", new BigDecimal("2.10"), t);
        String json = mapper.writeValueAsString(original);

        EventPayload parsed = EventPayload.fromJson(mapper, OddsUpdatedEvent.EVENT_TYPE, json);

        assertThat(parsed).isInstanceOf(OddsUpdatedEvent.class);
        OddsUpdatedEvent odds = (OddsUpdatedEvent) parsed;
        assertThat(odds.eventId()).isEqualTo("e1");
        assertThat(odds.selection()).isEqualTo("HOME");
        assertThat(odds.odds()).isEqualByComparingTo("2.10");
        assertThat(odds.updatedAt()).isEqualTo(t);
    }

    @Test
    void fromJsonUsesUntypedWrapperForUnknownEventType() {
        String json = "{\"foo\":1}";
        EventPayload parsed = EventPayload.fromJson(mapper, "some.other.v1", json);
        assertThat(parsed).isInstanceOf(UntypedOutboxPayload.class);
        assertThat(((UntypedOutboxPayload) parsed).raw().path("foo").asInt()).isEqualTo(1);
    }
}
