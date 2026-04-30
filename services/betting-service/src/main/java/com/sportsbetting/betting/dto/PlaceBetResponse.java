package com.sportsbetting.betting.dto;

import java.math.BigDecimal;

public record PlaceBetResponse(
        String betId,
        BigDecimal acceptedOdds,
        String status
) {
}
