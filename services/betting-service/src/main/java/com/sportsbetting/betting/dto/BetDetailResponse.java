package com.sportsbetting.betting.dto;

import java.math.BigDecimal;

public record BetDetailResponse(
        String betId,
        String userId,
        String eventId,
        String selection,
        BigDecimal stake,
        BigDecimal odds,
        String status
) {
}
