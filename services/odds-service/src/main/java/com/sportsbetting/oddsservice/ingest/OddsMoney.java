package com.sportsbetting.oddsservice.ingest;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class OddsMoney {

    private OddsMoney() {
    }

    public static BigDecimal normalize(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
