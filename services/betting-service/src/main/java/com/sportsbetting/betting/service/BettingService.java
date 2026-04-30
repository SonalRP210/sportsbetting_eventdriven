package com.sportsbetting.betting.service;

import com.sportsbetting.betting.dto.BetDetailResponse;
import com.sportsbetting.betting.dto.CancelBetResponse;
import com.sportsbetting.betting.dto.PlaceBetRequest;
import com.sportsbetting.betting.dto.PlaceBetResponse;
import com.sportsbetting.betting.model.Bet;
import com.sportsbetting.betting.model.BetStatus;
import com.sportsbetting.betting.model.DomainEvent;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BettingService {

    private static final String BET_PREFIX = "BET-";

    private final Map<String, Bet> betStore = new ConcurrentHashMap<>();
    private final Map<String, String> idempotencyIndex = new ConcurrentHashMap<>();
    private final Map<String, BigDecimal> oddsStore = new ConcurrentHashMap<>();
    private final List<DomainEvent> outbox = new ArrayList<>();

    public BettingService() {
        oddsStore.put(oddsKey("event-001", "HOME"), new BigDecimal("1.90"));
        oddsStore.put(oddsKey("event-001", "AWAY"), new BigDecimal("2.10"));
    }

    public void setOdds(String eventId, String selection, BigDecimal odds) {
        if (odds == null || odds.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Odds must be greater than zero");
        }
        oddsStore.put(oddsKey(eventId, selection), money(odds));
    }

    public PlaceBetResponse placeBet(PlaceBetRequest request, String idempotencyKey) {
        String normalizedKey = normalizeIdempotencyKey(idempotencyKey);
        if (normalizedKey != null) {
            String replayBetId = idempotencyIndex.get(request.userId() + "::" + normalizedKey);
            if (replayBetId != null) {
                Bet existing = betStore.get(replayBetId);
                return new PlaceBetResponse(existing.betId(), existing.odds(), existing.status().name());
            }
        }

        BigDecimal odds = oddsStore.get(oddsKey(request.eventId(), request.selection()));
        if (odds == null) {
            throw new IllegalArgumentException("No active odds found for event/selection");
        }

        Bet bet = new Bet(
                BET_PREFIX + UUID.randomUUID(),
                request.userId(),
                request.eventId(),
                request.selection(),
                money(request.stake()),
                money(odds),
                BetStatus.OPEN,
                normalizedKey
        );

        betStore.put(bet.betId(), bet);
        if (normalizedKey != null) {
            idempotencyIndex.put(request.userId() + "::" + normalizedKey, bet.betId());
        }

        BigDecimal openRisk = money(bet.stake().multiply(bet.odds()));
        outbox.add(new DomainEvent("betting.bet.placed.v1", Map.of(
                "betId", bet.betId(),
                "userId", bet.userId(),
                "eventId", bet.eventId(),
                "selection", bet.selection(),
                "stake", bet.stake(),
                "odds", bet.odds(),
                "openRisk", openRisk
        )));

        return new PlaceBetResponse(bet.betId(), bet.odds(), bet.status().name());
    }

    public Optional<BetDetailResponse> getBetById(String betId) {
        Bet bet = betStore.get(betId);
        if (bet == null) {
            return Optional.empty();
        }
        return Optional.of(new BetDetailResponse(
                bet.betId(),
                bet.userId(),
                bet.eventId(),
                bet.selection(),
                bet.stake(),
                bet.odds(),
                bet.status().name()
        ));
    }

    public CancelBetResponse cancelBet(String betId) {
        Bet bet = betStore.get(betId);
        if (bet == null) {
            throw new IllegalArgumentException("Bet not found");
        }
        if (bet.status() != BetStatus.OPEN) {
            throw new IllegalArgumentException("Only OPEN bets can be cancelled");
        }

        Bet cancelled = bet.withStatus(BetStatus.CANCELLED);
        betStore.put(cancelled.betId(), cancelled);

        BigDecimal openRisk = money(cancelled.stake().multiply(cancelled.odds()));
        outbox.add(new DomainEvent("betting.bet.cancelled.v1", Map.of(
                "betId", cancelled.betId(),
                "userId", cancelled.userId(),
                "openRisk", openRisk
        )));

        return new CancelBetResponse(cancelled.betId(), cancelled.status().name(), "Bet cancelled");
    }

    public List<DomainEvent> outboxEvents() {
        return List.copyOf(outbox);
    }

    public void resetForTests() {
        betStore.clear();
        idempotencyIndex.clear();
        outbox.clear();
    }

    private String normalizeIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null) {
            return null;
        }
        String trimmed = idempotencyKey.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String oddsKey(String eventId, String selection) {
        return eventId + "::" + selection;
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
