package com.sportsbetting.oddsservice.model.odds;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record OddsUpdate(
        @NotBlank(message = "eventId must not be blank")
        String eventId,

        @NotBlank(message = "selection must not be blank")
        String selection,

        @NotNull(message = "odds must not be null")
        @DecimalMin(value = "0.01", message = "odds must be greater than zero")
        BigDecimal odds
) {
}
