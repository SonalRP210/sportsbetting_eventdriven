package com.sportsbetting.settlementservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportsbetting.settlementservice.dto.SettleEventResponse;
import com.sportsbetting.settlementservice.model.*;
import com.sportsbetting.settlementservice.repository.BetPositionRepository;
import com.sportsbetting.settlementservice.repository.EventSettlementRepository;
import com.sportsbetting.settlementservice.repository.OutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

@Service
public class SettlementService {
    private final BetPositionRepository betPositionRepository;
    private final EventSettlementRepository eventSettlementRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public SettlementService(BetPositionRepository betPositionRepository, EventSettlementRepository eventSettlementRepository, OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.betPositionRepository = betPositionRepository;
        this.eventSettlementRepository = eventSettlementRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public SettleEventResponse settleEvent(String eventId, String winningSelection) {
        EventSettlementEntity prior = eventSettlementRepository.findById(eventId).orElse(null);
        if (prior != null) {
            if (!prior.getWinningSelection().equals(winningSelection)) throw new IllegalStateException("SETTLEMENT_CONFLICT");
            return new SettleEventResponse(prior.getEventId(), prior.getWinningSelection(), prior.getWinners(), prior.getLosers(), prior.getTotalPayout(), currentGlobalExposure());
        }

        List<BetPositionEntity> openBets = betPositionRepository.findByEventIdAndStatus(eventId, "OPEN");
        if (openBets.isEmpty()) return new SettleEventResponse(eventId, winningSelection, 0, 0, bd("0.00"), currentGlobalExposure());

        int winners = 0;
        int losers = 0;
        BigDecimal payout = bd("0.00");
        List<Map<String, Object>> releases = new ArrayList<>();

        for (BetPositionEntity bet : openBets) {
            BigDecimal risk = money(bet.getStake().multiply(bet.getOdds()));
            releases.add(Map.of("userId", bet.getUserId(), "openRisk", risk));
            if (bet.getSelection().equals(winningSelection)) {
                bet.setStatus("WON");
                winners++;
                payout = money(payout.add(risk));
            } else {
                bet.setStatus("LOST");
                losers++;
            }
            betPositionRepository.save(bet);
        }

        EventSettlementEntity ledger = new EventSettlementEntity();
        ledger.setEventId(eventId);
        ledger.setWinningSelection(winningSelection);
        ledger.setWinners(winners);
        ledger.setLosers(losers);
        ledger.setTotalPayout(payout);
        eventSettlementRepository.save(ledger);

        persistOutbox("settlement.event.settled.v1", Map.of("eventId", eventId, "winningSelection", winningSelection, "releases", releases));
        return new SettleEventResponse(eventId, winningSelection, winners, losers, payout, currentGlobalExposure());
    }

    @Transactional
    public void seedOpenPosition(BetPosition betPosition) {
        BetPositionEntity entity = new BetPositionEntity();
        entity.setBetId(betPosition.betId());
        entity.setUserId(betPosition.userId());
        entity.setEventId(betPosition.eventId());
        entity.setSelection(betPosition.selection());
        entity.setStake(money(betPosition.stake()));
        entity.setOdds(money(betPosition.odds()));
        entity.setStatus(betPosition.status());
        betPositionRepository.save(entity);
    }

    @Transactional
    public void onBetPlaced(String betId, String userId, String eventId, String selection, BigDecimal stake, BigDecimal odds) {
        BetPositionEntity entity = betPositionRepository.findById(betId).orElseGet(BetPositionEntity::new);
        entity.setBetId(betId);
        entity.setUserId(userId);
        entity.setEventId(eventId);
        entity.setSelection(selection);
        entity.setStake(money(stake));
        entity.setOdds(money(odds));
        entity.setStatus("OPEN");
        betPositionRepository.save(entity);
    }

    @Transactional
    public void onBetCancelled(String betId) {
        betPositionRepository.findById(betId).ifPresent(entity -> {
            if ("OPEN".equals(entity.getStatus())) {
                entity.setStatus("CANCELLED");
                betPositionRepository.save(entity);
            }
        });
    }

    @Transactional(readOnly = true)
    public Optional<BetPosition> getPosition(String betId) {
        return betPositionRepository.findById(betId).map(this::toRecord);
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
        eventSettlementRepository.deleteAll();
        betPositionRepository.deleteAll();
    }

    @Transactional(readOnly = true)
    public BigDecimal currentGlobalExposure() {
        return money(betPositionRepository.findAll().stream()
                .filter(b -> "OPEN".equals(b.getStatus()))
                .map(b -> money(b.getStake().multiply(b.getOdds())))
                .reduce(bd("0.00"), BigDecimal::add));
    }

    private BetPosition toRecord(BetPositionEntity e) {
        return new BetPosition(e.getBetId(), e.getUserId(), e.getEventId(), e.getSelection(), money(e.getStake()), money(e.getOdds()), e.getStatus());
    }

    private void persistOutbox(String eventType, Map<String, Object> payload) {
        OutboxEventEntity outbox = new OutboxEventEntity();
        outbox.setId(UUID.randomUUID());
        outbox.setEventType(eventType);
        outbox.setPayload(writeJson(payload));
        outbox.setPublished(false);
        outbox.setCreatedAt(Instant.now());
        outboxEventRepository.save(outbox);
    }

    private Map<String, Object> parsePayload(String payload) {
        try { return objectMapper.readValue(payload, LinkedHashMap.class); }
        catch (JsonProcessingException e) { throw new IllegalStateException(e); }
    }
    private String writeJson(Map<String, Object> payload) {
        try { return objectMapper.writeValueAsString(payload); }
        catch (JsonProcessingException e) { throw new IllegalStateException(e); }
    }
    private BigDecimal bd(String value) { return new BigDecimal(value).setScale(2, RoundingMode.HALF_UP); }
    private BigDecimal money(BigDecimal value) { return value.setScale(2, RoundingMode.HALF_UP); }
}
