package com.sportsbetting.betting.model;

import java.math.BigDecimal;

public record Bet(
        String betId,
        String userId,
        String eventId,
        String selection,
        BigDecimal stake,
        BigDecimal odds,
        BetStatus status,
        String idempotencyKey
) {
    public Bet withStatus(BetStatus newStatus) {
        return new Bet(betId, userId, eventId, selection, stake, odds, newStatus, idempotencyKey);
    }
}
