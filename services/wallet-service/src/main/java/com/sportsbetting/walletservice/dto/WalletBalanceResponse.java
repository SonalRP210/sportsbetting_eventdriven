package com.sportsbetting.walletservice.dto;

import java.math.BigDecimal;

public record WalletBalanceResponse(
        String walletId,
        String userId,
        BigDecimal balance,
        String currency
) {
}
