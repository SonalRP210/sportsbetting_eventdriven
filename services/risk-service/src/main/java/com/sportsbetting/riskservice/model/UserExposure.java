package com.sportsbetting.riskservice.model;

import java.math.BigDecimal;

public record UserExposure(
        String userId,
        BigDecimal openRisk,
        int openBetCount
) {
}
