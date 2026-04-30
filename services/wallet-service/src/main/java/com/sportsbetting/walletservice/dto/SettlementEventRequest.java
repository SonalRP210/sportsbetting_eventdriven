package com.sportsbetting.walletservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SettlementEventRequest(
        @NotBlank String eventId,
        @NotBlank String winningSelection,
        @NotEmpty List<SettlementRelease> releases
) {
}
