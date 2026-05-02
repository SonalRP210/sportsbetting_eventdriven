package com.sportsbetting.oddsservice.model.odds;

import com.sportsbetting.oddsservice.model.events.EventPayload;

import java.math.BigDecimal;
import java.time.Instant;
/**
 * Typed outbound payload for {@value #EVENT_TYPE}. Serialized to JSON for the transactional outbox.
 */
public record OddsUpdatedEvent(
        String eventId,
        String selection,
        BigDecimal odds,
        Instant updatedAt
) implements EventPayload {
    public static final String EVENT_TYPE = "odds.updated.v1";

    public static OddsUpdatedEvent from(OddsUpdate update, Instant updatedAt, BigDecimal normalizedOdds) {
        return new OddsUpdatedEvent(update.eventId(), update.selection(), normalizedOdds, updatedAt);
    }
}
