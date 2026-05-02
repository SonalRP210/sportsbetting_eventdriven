package com.sportsbetting.oddsservice.api.dto;

/**
 * Body for {@code POST /api/v1/internal/seed-odds} ({@code 202}).
 */
public record SeedOddsResponse(boolean accepted) {

    public static SeedOddsResponse ok() {
        return new SeedOddsResponse(true);
    }
}
