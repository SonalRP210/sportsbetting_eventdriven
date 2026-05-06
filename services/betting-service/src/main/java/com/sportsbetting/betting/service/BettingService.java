package com.sportsbetting.betting.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportsbetting.betting.dto.BetDetailResponse;
import com.sportsbetting.betting.dto.CancelBetResponse;
import com.sportsbetting.betting.dto.PlaceBetRequest;
import com.sportsbetting.betting.dto.PlaceBetResponse;
import com.sportsbetting.betting.dto.UserBetSummaryResponse;
import com.sportsbetting.betting.model.BetEntity;
import com.sportsbetting.betting.model.BetStatus;
import com.sportsbetting.betting.model.DomainEvent;
import com.sportsbetting.betting.model.OddsQuoteEntity;
import com.sportsbetting.betting.model.OutboxEventEntity;
import com.sportsbetting.betting.repository.BetRepository;
import com.sportsbetting.betting.repository.OddsQuoteRepository;
import com.sportsbetting.betting.repository.OutboxEventRepository;
import com.sportsbetting.platform.messaging.EventPayloadValidator;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Spring-injected collaborators")
public class BettingService {

    private static final String BET_PREFIX = "BET-";

    private final BetRepository betRepository;
    private final OddsQuoteRepository oddsQuoteRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<EventPayloadValidator> eventSchemaValidator;

    public BettingService(
            BetRepository betRepository,
            OddsQuoteRepository oddsQuoteRepository,
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper,
            ObjectProvider<EventPayloadValidator> eventSchemaValidator) {
        this.betRepository = betRepository;
        this.oddsQuoteRepository = oddsQuoteRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper.copy();
        this.eventSchemaValidator = eventSchemaValidator;
    }

    @Transactional
    public void setOdds(String eventId, String selection, BigDecimal odds) {
        if (odds == null || odds.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Odds must be greater than zero");
        }
        String key = oddsKey(eventId, selection);
        OddsQuoteEntity quote = oddsQuoteRepository.findById(key).orElseGet(OddsQuoteEntity::new);
        quote.setKeyId(key);
        quote.setOddsKey(key);
        quote.setEventId(eventId);
        quote.setSelection(selection);
        quote.setOdds(money(odds));
        quote.setUpdatedAt(Instant.now());
        oddsQuoteRepository.save(quote);
    }

    @Transactional
    public PlaceBetResponse placeBet(PlaceBetRequest request, String idempotencyKey) {
        String normalizedKey = normalizeIdempotencyKey(idempotencyKey);
        if (normalizedKey != null) {
            Optional<BetEntity> replay = betRepository.findByUserIdAndIdempotencyKey(request.userId(), normalizedKey);
            if (replay.isPresent()) {
                BetEntity existing = replay.get();
                return new PlaceBetResponse(existing.getBetId(), existing.getOdds(), existing.getStatus().name());
            }
        }

        BigDecimal odds = oddsQuoteRepository.findById(oddsKey(request.eventId(), request.selection()))
                .map(OddsQuoteEntity::getOdds)
                .orElseThrow(() -> new IllegalArgumentException("No active odds found for event/selection"));

        BetEntity bet = new BetEntity();
        bet.setBetId(BET_PREFIX + UUID.randomUUID());
        bet.setUserId(request.userId());
        bet.setEventId(request.eventId());
        bet.setSelection(request.selection());
        bet.setStake(money(request.stake()));
        bet.setOdds(money(odds));
        bet.setStatus(BetStatus.OPEN);
        bet.setIdempotencyKey(normalizedKey);
        betRepository.save(bet);

        BigDecimal openRisk = money(bet.getStake().multiply(bet.getOdds()));
        persistOutbox(
                bet.getBetId(),
                "betting.bet.placed.v1",
                Map.of(
                        "betId", bet.getBetId(),
                        "userId", bet.getUserId(),
                        "eventId", bet.getEventId(),
                        "selection", bet.getSelection(),
                        "stake", bet.getStake(),
                        "odds", bet.getOdds(),
                        "openRisk", openRisk
                ));

        return new PlaceBetResponse(bet.getBetId(), bet.getOdds(), bet.getStatus().name());
    }

    @Transactional(readOnly = true)
    public Optional<BetDetailResponse> getBetById(String betId) {
        return betRepository.findById(betId)
                .map(b -> new BetDetailResponse(
                        b.getBetId(),
                        b.getUserId(),
                        b.getEventId(),
                        b.getSelection(),
                        b.getStake(),
                        b.getOdds(),
                        b.getStatus().name()
                ));
    }

    @Transactional(readOnly = true)
    public List<UserBetSummaryResponse> getUserBets(String userId, int page, int size) {
        return slice(betRepository.findByUserIdOrderByBetIdAsc(userId), page, size).stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserBetSummaryResponse> getEventBets(String eventId, int page, int size) {
        return slice(betRepository.findByEventIdOrderByBetIdAsc(eventId), page, size).stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional
    public CancelBetResponse cancelBet(String betId) {
        BetEntity bet = betRepository.findById(betId)
                .orElseThrow(() -> new IllegalArgumentException("Bet not found"));

        if (bet.getStatus() != BetStatus.OPEN) {
            throw new IllegalArgumentException("Only OPEN bets can be cancelled");
        }

        bet.setStatus(BetStatus.CANCELLED);
        betRepository.save(bet);

        BigDecimal openRisk = money(bet.getStake().multiply(bet.getOdds()));
        persistOutbox(
                bet.getBetId(),
                "betting.bet.cancelled.v1",
                Map.of(
                        "betId", bet.getBetId(),
                        "userId", bet.getUserId(),
                        "openRisk", openRisk
                ));

        return new CancelBetResponse(bet.getBetId(), bet.getStatus().name(), "Bet cancelled");
    }

    @Transactional(readOnly = true)
    public List<DomainEvent> outboxEvents() {
        return outboxEventRepository.findAll().stream().map(this::toDomainEvent).toList();
    }

    /**
     * Projects {@code settlement.event.settled.v1} onto local bets: OPEN positions become WON or LOST.
     */
    @Transactional
    public void applyEventSettlement(String eventId, String winningSelection) {
        if (eventId == null || eventId.isBlank() || winningSelection == null || winningSelection.isBlank()) {
            throw new IllegalArgumentException("eventId and winningSelection are required");
        }
        List<BetEntity> openBets = betRepository.findByEventIdAndStatus(eventId, BetStatus.OPEN);
        for (BetEntity bet : openBets) {
            if (winningSelection.equals(bet.getSelection())) {
                bet.setStatus(BetStatus.WON);
            } else {
                bet.setStatus(BetStatus.LOST);
            }
            betRepository.save(bet);
        }
    }

    @Transactional
    public void resetForTests() {
        betRepository.deleteAll();
        outboxEventRepository.deleteAll();
        oddsQuoteRepository.deleteAll();
        setOdds("event-001", "HOME", new BigDecimal("1.90"));
        setOdds("event-001", "AWAY", new BigDecimal("2.10"));
    }

    private UserBetSummaryResponse toSummary(BetEntity bet) {
        return new UserBetSummaryResponse(bet.getBetId(), bet.getEventId(), bet.getStake(), bet.getOdds(), bet.getStatus().name());
    }

    private List<BetEntity> slice(List<BetEntity> input, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);
        int from = safePage * safeSize;
        if (from >= input.size()) {
            return List.of();
        }
        int to = Math.min(from + safeSize, input.size());
        return input.subList(from, to);
    }

    private void persistOutbox(String messageKey, String eventType, Map<String, Object> payload) {
        String json = writeJson(payload);
        eventSchemaValidator.ifAvailable(v -> v.validateIfPresent(eventType, json));
        OutboxEventEntity row = new OutboxEventEntity();
        row.setId(UUID.randomUUID());
        row.setEventType(eventType);
        row.setPayload(json);
        row.setMessageKey(messageKey);
        row.setPublished(false);
        row.setCreatedAt(Instant.now());
        outboxEventRepository.save(row);
    }

    private DomainEvent toDomainEvent(OutboxEventEntity e) {
        return new DomainEvent(e.getEventType(), readJson(e.getPayload()));
    }

    private Object readJson(String payload) {
        try {
            return objectMapper.readValue(payload, Object.class);
        } catch (JsonProcessingException ex) {
            return payload;
        }
    }

    private String writeJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize outbox payload", ex);
        }
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


