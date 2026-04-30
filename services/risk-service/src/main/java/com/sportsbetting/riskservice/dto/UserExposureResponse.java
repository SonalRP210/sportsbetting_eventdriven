package com.sportsbetting.riskservice.dto;

import java.math.BigDecimal;

public record UserExposureResponse(
        String userId,
        BigDecimal openRisk,
        int openBetCount
) {
}
