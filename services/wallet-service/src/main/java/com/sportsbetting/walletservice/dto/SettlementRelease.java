package com.sportsbetting.walletservice.dto;

import java.math.BigDecimal;

public record SettlementRelease(
        String userId,
        BigDecimal openRisk
) {
}
