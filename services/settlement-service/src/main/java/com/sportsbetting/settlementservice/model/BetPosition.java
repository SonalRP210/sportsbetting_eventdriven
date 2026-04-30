package com.sportsbetting.settlementservice.model;

import java.math.BigDecimal;

public record BetPosition(
        String betId,
        String userId,
        String eventId,
        String selection,
        BigDecimal stake,
        BigDecimal odds,
        String status
) {
    public BetPosition settled(String nextStatus) {
        return new BetPosition(betId, userId, eventId, selection, stake, odds, nextStatus);
    }
}
