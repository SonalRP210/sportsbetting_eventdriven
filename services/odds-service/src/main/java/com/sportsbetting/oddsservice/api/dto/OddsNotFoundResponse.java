package com.sportsbetting.oddsservice.api.dto;

/**
 * Body for {@code GET /api/v1/odds/{eventId}/{selection}} when no quote exists ({@code 404}).
 */
public record OddsNotFoundResponse(String error) implements OddsLookupResponse {

    public static OddsNotFoundResponse oddsNotFound() {
        return new OddsNotFoundResponse("odds_not_found");
    }
}
