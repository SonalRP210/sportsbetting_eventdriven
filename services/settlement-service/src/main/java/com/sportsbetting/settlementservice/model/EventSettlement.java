package com.sportsbetting.settlementservice.model;

import java.math.BigDecimal;

public record EventSettlement(
        String eventId,
        String winningSelection,
        int winners,
        int losers,
        BigDecimal totalPayout
) {
}
