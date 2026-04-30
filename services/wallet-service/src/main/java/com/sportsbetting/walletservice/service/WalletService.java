package com.sportsbetting.walletservice.service;

import com.sportsbetting.walletservice.dto.SettlementEventRequest;
import com.sportsbetting.walletservice.dto.SettlementRelease;
import com.sportsbetting.walletservice.dto.WalletBalanceResponse;
import com.sportsbetting.walletservice.dto.WalletTransactionRequest;
import com.sportsbetting.walletservice.model.DomainEvent;
import com.sportsbetting.walletservice.model.TransactionType;
import com.sportsbetting.walletservice.model.WalletTransaction;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WalletService {

    private final Map<String, BigDecimal> balances = new ConcurrentHashMap<>();
    private final Map<String, String> walletOwners = new ConcurrentHashMap<>();
    private final List<WalletTransaction> transactions = new ArrayList<>();
    private final List<DomainEvent> outbox = new ArrayList<>();

    public WalletTransaction postTransaction(WalletTransactionRequest request) {
        TransactionType type = TransactionType.valueOf(request.type().toUpperCase());
        BigDecimal amount = money(request.amount());
        String walletId = request.walletId();
        String userId = request.userId();

        walletOwners.putIfAbsent(walletId, userId);
        BigDecimal current = balances.getOrDefault(walletId, money(BigDecimal.ZERO));
        BigDecimal next = switch (type) {
            case CREDIT, PAYOUT, REFUND -> money(current.add(amount));
            case DEBIT -> money(current.subtract(amount));
        };

        if (next.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("INSUFFICIENT_FUNDS");
        }

        balances.put(walletId, next);
        WalletTransaction tx = new WalletTransaction(
                UUID.randomUUID().toString(),
                walletId,
                userId,
                amount,
                request.currency(),
                type,
                request.reference(),
                Instant.now()
        );
        transactions.add(tx);

        String eventType = switch (type) {
            case DEBIT -> "wallet.wallet.debited.v1";
            case CREDIT, PAYOUT, REFUND -> "wallet.wallet.credited.v1";
        };

        outbox.add(new DomainEvent(eventType, Map.of(
                "walletId", tx.walletId(),
                "userId", tx.userId(),
                "amount", tx.amount(),
                "currency", tx.currency(),
                "transactionId", tx.transactionId(),
                "type", tx.type().name()
        )));

        return tx;
    }

    public WalletBalanceResponse getBalance(String userId) {
        Optional<Map.Entry<String, String>> walletEntry = walletOwners.entrySet().stream()
                .filter(e -> e.getValue().equals(userId))
                .findFirst();

        String walletId = walletEntry.map(Map.Entry::getKey).orElse("wallet-" + userId);
        walletOwners.putIfAbsent(walletId, userId);
        BigDecimal balance = balances.getOrDefault(walletId, money(BigDecimal.ZERO));
        return new WalletBalanceResponse(walletId, userId, balance, "USD");
    }

    public int consumeSettlementEvent(SettlementEventRequest event) {
        int posted = 0;
        for (SettlementRelease release : event.releases()) {
            String userId = release.userId();
            String walletId = walletOwners.entrySet().stream()
                    .filter(e -> e.getValue().equals(userId))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse("wallet-" + userId);

            walletOwners.putIfAbsent(walletId, userId);
            postTransaction(new WalletTransactionRequest(
                    walletId,
                    userId,
                    release.openRisk(),
                    "USD",
                    "PAYOUT",
                    "settlement:" + event.eventId()
            ));
            posted++;
        }
        return posted;
    }

    public List<DomainEvent> outboxEvents() {
        return List.copyOf(outbox);
    }

    public void resetForTests() {
        balances.clear();
        walletOwners.clear();
        transactions.clear();
        outbox.clear();
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
