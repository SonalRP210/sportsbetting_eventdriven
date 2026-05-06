package com.sportsbetting.oddsservice.api.dto;

import java.math.BigDecimal;

/**
 * Body for {@code GET /api/v1/odds/{eventId}/{selection}} when odds exist ({@code 200}).
 */
public record OddsQuoteResponse(String eventId, String selection, BigDecimal odds) implements OddsLookupResponse {
}
