package com.sportsbetting.riskservice.dto;

import java.math.BigDecimal;
import java.util.List;

public record EventSettledRequest(
        String eventId,
        String winningSelection,
        List<RiskRelease> releases
) {
    public record RiskRelease(String userId, BigDecimal openRisk) {}
}
