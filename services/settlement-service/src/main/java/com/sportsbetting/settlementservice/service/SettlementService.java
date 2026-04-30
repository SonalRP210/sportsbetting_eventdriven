package com.sportsbetting.settlementservice.service;

import com.sportsbetting.settlementservice.dto.SettleEventResponse;
import com.sportsbetting.settlementservice.model.BetPosition;
import com.sportsbetting.settlementservice.model.DomainEvent;
import com.sportsbetting.settlementservice.model.EventSettlement;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class SettlementService {

    private final Map<String, BetPosition> positions = new ConcurrentHashMap<>();
    private final Map<String, EventSettlement> settlementLedger = new ConcurrentHashMap<>();
    private final List<DomainEvent> outbox = new ArrayList<>();

    public SettlementService() {
        seedOpenPosition(new BetPosition("BET-1", "user-1", "event-001", "HOME", bd("100.00"), bd("1.95"), "OPEN"));
        seedOpenPosition(new BetPosition("BET-2", "user-2", "event-001", "AWAY", bd("80.00"), bd("2.10"), "OPEN"));
    }

    public SettleEventResponse settleEvent(String eventId, String winningSelection) {
        EventSettlement prior = settlementLedger.get(eventId);
        if (prior != null) {
            if (!prior.winningSelection().equals(winningSelection)) {
                throw new IllegalStateException("SETTLEMENT_CONFLICT");
            }
            return new SettleEventResponse(
                    prior.eventId(),
                    prior.winningSelection(),
                    prior.winners(),
                    prior.losers(),
                    prior.totalPayout(),
                    currentGlobalExposure()
            );
        }

        List<BetPosition> openBets = positions.values().stream()
                .filter(b -> b.eventId().equals(eventId) && "OPEN".equals(b.status()))
                .collect(Collectors.toList());

        if (openBets.isEmpty()) {
            return new SettleEventResponse(eventId, winningSelection, 0, 0, bd("0.00"), currentGlobalExposure());
        }

        int winners = 0;
        int losers = 0;
        BigDecimal payout = bd("0.00");
        List<Map<String, Object>> releases = new ArrayList<>();

        for (BetPosition bet : openBets) {
            BigDecimal risk = money(bet.stake().multiply(bet.odds()));
            releases.add(Map.of("userId", bet.userId(), "openRisk", risk));

            if (bet.selection().equals(winningSelection)) {
                positions.put(bet.betId(), bet.settled("WON"));
                winners++;
                payout = money(payout.add(risk));
            } else {
                positions.put(bet.betId(), bet.settled("LOST"));
                losers++;
            }
        }

        EventSettlement ledger = new EventSettlement(eventId, winningSelection, winners, losers, payout);
        settlementLedger.put(eventId, ledger);

        outbox.add(new DomainEvent("settlement.event.settled.v1", Map.of(
                "eventId", eventId,
                "winningSelection", winningSelection,
                "releases", releases
        )));

        return new SettleEventResponse(eventId, winningSelection, winners, losers, payout, currentGlobalExposure());
    }

    public void seedOpenPosition(BetPosition betPosition) {
        positions.put(betPosition.betId(), betPosition);
    }

    public Optional<BetPosition> getPosition(String betId) {
        return Optional.ofNullable(positions.get(betId));
    }

    public List<DomainEvent> outboxEvents() {
        return List.copyOf(outbox);
    }

    public void resetForTests() {
        positions.clear();
        settlementLedger.clear();
        outbox.clear();
    }

    private BigDecimal currentGlobalExposure() {
        return money(positions.values().stream()
                .filter(b -> "OPEN".equals(b.status()))
                .map(b -> money(b.stake().multiply(b.odds())))
                .reduce(bd("0.00"), BigDecimal::add));
    }

    private BigDecimal bd(String value) {
        return new BigDecimal(value).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
