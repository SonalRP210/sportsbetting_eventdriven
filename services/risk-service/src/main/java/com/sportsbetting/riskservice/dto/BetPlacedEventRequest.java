package com.sportsbetting.riskservice.dto;

import java.math.BigDecimal;

public record BetPlacedEventRequest(
        String betId,
        String userId,
        BigDecimal openRisk
) {
}
