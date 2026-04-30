package com.sportsbetting.settlementservice.dto;

import java.math.BigDecimal;

public record SettleEventResponse(
        String eventId,
        String winningSelection,
        int winners,
        int losers,
        BigDecimal totalPayout,
        BigDecimal globalExposure
) {
}
