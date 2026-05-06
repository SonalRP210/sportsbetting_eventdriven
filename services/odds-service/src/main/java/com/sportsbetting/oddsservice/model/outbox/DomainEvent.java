package com.sportsbetting.oddsservice.model.outbox;

import com.sportsbetting.oddsservice.model.events.EventPayload;

/**
 * View of one {@code outbox_events} row for operator/debug APIs.
 *
 * @param type    Matches {@link OutboxEventEntity#getEventType()}.
 * @param payload Parsed JSON — {@link com.sportsbetting.oddsservice.model.odds.OddsUpdatedEvent} when {@code type}
 *                is {@value com.sportsbetting.oddsservice.model.odds.OddsUpdatedEvent#EVENT_TYPE}.
 */
public record DomainEvent(String type, EventPayload payload) {
}
