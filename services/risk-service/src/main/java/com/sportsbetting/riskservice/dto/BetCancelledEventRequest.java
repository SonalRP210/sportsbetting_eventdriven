package com.sportsbetting.riskservice.dto;

import java.math.BigDecimal;

public record BetCancelledEventRequest(
        String betId,
        String userId,
        BigDecimal openRisk
) {
}
