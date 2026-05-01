package com.sportsbetting.walletservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportsbetting.walletservice.dto.SettlementEventRequest;
import com.sportsbetting.walletservice.dto.SettlementRelease;
import com.sportsbetting.walletservice.dto.WalletBalanceResponse;
import com.sportsbetting.walletservice.dto.WalletTransactionRequest;
import com.sportsbetting.walletservice.model.DomainEvent;
import com.sportsbetting.walletservice.model.OutboxEventEntity;
import com.sportsbetting.walletservice.model.TransactionType;
import com.sportsbetting.walletservice.model.WalletAccountEntity;
import com.sportsbetting.walletservice.model.WalletTransaction;
import com.sportsbetting.walletservice.model.WalletTransactionEntity;
import com.sportsbetting.walletservice.repository.OutboxEventRepository;
import com.sportsbetting.walletservice.repository.WalletAccountRepository;
import com.sportsbetting.walletservice.repository.WalletTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class WalletService {

    private final WalletAccountRepository walletAccountRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public WalletService(
            WalletAccountRepository walletAccountRepository,
            WalletTransactionRepository walletTransactionRepository,
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper
    ) {
        this.walletAccountRepository = walletAccountRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public WalletTransaction postTransaction(WalletTransactionRequest request) {
        TransactionType type = TransactionType.valueOf(request.type().toUpperCase());
        BigDecimal amount = money(request.amount());
        WalletAccountEntity account = walletAccountRepository.findById(request.walletId())
                .orElseGet(() -> newAccount(request.walletId(), request.userId(), request.currency()));

        BigDecimal current = money(account.getBalance());
        BigDecimal next = switch (type) {
            case CREDIT, PAYOUT, REFUND -> money(current.add(amount));
            case DEBIT -> money(current.subtract(amount));
        };
        if (next.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("INSUFFICIENT_FUNDS");
        }

        account.setBalance(next);
        walletAccountRepository.save(account);

        WalletTransactionEntity tx = new WalletTransactionEntity();
        tx.setTransactionId(UUID.randomUUID().toString());
        tx.setWalletId(account.getWalletId());
        tx.setUserId(account.getUserId());
        tx.setAmount(amount);
        tx.setCurrency(account.getCurrency());
        tx.setType(type);
        tx.setReference(request.reference());
        tx.setCreatedAt(Instant.now());
        walletTransactionRepository.save(tx);

        String eventType = (type == TransactionType.DEBIT) ? "wallet.wallet.debited.v1" : "wallet.wallet.credited.v1";
        persistOutbox(eventType, Map.of(
                "walletId", tx.getWalletId(),
                "userId", tx.getUserId(),
                "amount", tx.getAmount(),
                "currency", tx.getCurrency(),
                "transactionId", tx.getTransactionId(),
                "type", tx.getType().name()
        ));

        return toRecord(tx);
    }

    @Transactional(readOnly = true)
    public WalletBalanceResponse getBalance(String userId) {
        WalletAccountEntity account = walletAccountRepository.findByUserId(userId)
                .orElseGet(() -> {
                    WalletAccountEntity created = newAccount("wallet-" + userId, userId, "USD");
                    created.setBalance(money(BigDecimal.ZERO));
                    return walletAccountRepository.save(created);
                });
        return new WalletBalanceResponse(account.getWalletId(), account.getUserId(), money(account.getBalance()), account.getCurrency());
    }

    @Transactional
    public int consumeSettlementEvent(SettlementEventRequest event) {
        int posted = 0;
        for (SettlementRelease release : event.releases()) {
            WalletAccountEntity account = walletAccountRepository.findByUserId(release.userId())
                    .orElseGet(() -> walletAccountRepository.save(newAccount("wallet-" + release.userId(), release.userId(), "USD")));
            postTransaction(new WalletTransactionRequest(
                    account.getWalletId(),
                    release.userId(),
                    release.openRisk(),
                    account.getCurrency(),
                    "PAYOUT",
                    "settlement:" + event.eventId()
            ));
            posted++;
        }
        return posted;
    }

    @Transactional(readOnly = true)
    public List<DomainEvent> outboxEvents() {
        List<DomainEvent> events = new ArrayList<>();
        for (OutboxEventEntity outbox : outboxEventRepository.findAll()) {
            events.add(new DomainEvent(outbox.getEventType(), parsePayload(outbox.getPayload())));
        }
        return events;
    }

    @Transactional
    public void resetForTests() {
        outboxEventRepository.deleteAll();
        walletTransactionRepository.deleteAll();
        walletAccountRepository.deleteAll();
    }

    private WalletAccountEntity newAccount(String walletId, String userId, String currency) {
        WalletAccountEntity account = new WalletAccountEntity();
        account.setWalletId(walletId);
        account.setUserId(userId);
        account.setCurrency(currency == null ? "USD" : currency);
        account.setBalance(money(BigDecimal.ZERO));
        return account;
    }

    private void persistOutbox(String eventType, Map<String, Object> payload) {
        OutboxEventEntity outboxEvent = new OutboxEventEntity();
        outboxEvent.setId(UUID.randomUUID());
        outboxEvent.setEventType(eventType);
        outboxEvent.setPayload(writeJson(payload));
        outboxEvent.setPublished(false);
        outboxEvent.setCreatedAt(Instant.now());
        outboxEventRepository.save(outboxEvent);
    }

    private Map<String, Object> parsePayload(String payload) {
        try {
            return objectMapper.readValue(payload, LinkedHashMap.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to parse outbox payload", e);
        }
    }

    private String writeJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to write outbox payload", e);
        }
    }

    private WalletTransaction toRecord(WalletTransactionEntity entity) {
        return new WalletTransaction(
                entity.getTransactionId(),
                entity.getWalletId(),
                entity.getUserId(),
                money(entity.getAmount()),
                entity.getCurrency(),
                entity.getType(),
                entity.getReference(),
                entity.getCreatedAt()
        );
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
