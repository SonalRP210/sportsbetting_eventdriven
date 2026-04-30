package com.sportsbetting.betting.dto;

public record CancelBetResponse(
        String betId,
        String status,
        String message
) {
}
