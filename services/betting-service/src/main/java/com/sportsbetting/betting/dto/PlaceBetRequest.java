package com.sportsbetting.betting.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PlaceBetRequest(
        @NotBlank String userId,
        @NotBlank String eventId,
        @NotBlank String selection,
        @NotNull @DecimalMin(value = "0.01") BigDecimal stake
) {
}
