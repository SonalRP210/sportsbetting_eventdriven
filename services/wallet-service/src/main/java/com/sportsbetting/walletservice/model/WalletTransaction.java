package com.sportsbetting.walletservice.model;

import java.math.BigDecimal;
import java.time.Instant;

public record WalletTransaction(
        String transactionId,
        String walletId,
        String userId,
        BigDecimal amount,
        String currency,
        TransactionType type,
        String reference,
        Instant createdAt
) {
}
