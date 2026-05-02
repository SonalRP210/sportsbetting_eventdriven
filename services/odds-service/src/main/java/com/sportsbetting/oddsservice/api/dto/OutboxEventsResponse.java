package com.sportsbetting.oddsservice.api.dto;

import com.sportsbetting.oddsservice.model.outbox.DomainEvent;

import java.util.List;

/**
 * Body for {@code GET /api/v1/internal/outbox}.
 */
public record OutboxEventsResponse(List<DomainEvent> events) {
}
