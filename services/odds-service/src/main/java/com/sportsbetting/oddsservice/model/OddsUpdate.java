package com.sportsbetting.oddsservice.model;

import java.math.BigDecimal;

public record OddsUpdate(
        String eventId,
        String selection,
        BigDecimal odds
) {
}
