package com.sportsbetting.oddsservice.api.dto;

/**
 * Body for {@code POST /api/v1/odds-feed} when accepted ({@code 202}).
 */
public record OddsFeedAcceptedResponse(String message) {

    public static OddsFeedAcceptedResponse accepted() {
        return new OddsFeedAcceptedResponse("Odds feed accepted");
    }
}
