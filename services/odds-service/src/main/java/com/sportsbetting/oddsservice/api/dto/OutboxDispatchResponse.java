package com.sportsbetting.oddsservice.api.dto;

/**
 * Body for {@code POST /api/v1/internal/outbox/dispatch}.
 */
public record OutboxDispatchResponse(int dispatched) {
}
