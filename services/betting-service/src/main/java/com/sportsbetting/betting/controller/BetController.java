package com.sportsbetting.betting.controller;

import com.sportsbetting.betting.dto.BetDetailResponse;
import com.sportsbetting.betting.dto.CancelBetResponse;
import com.sportsbetting.betting.dto.PlaceBetRequest;
import com.sportsbetting.betting.dto.PlaceBetResponse;
import com.sportsbetting.betting.dto.UserBetSummaryResponse;
import com.sportsbetting.betting.model.DomainEvent;
import com.sportsbetting.betting.outbox.OutboxDispatcher;
import com.sportsbetting.betting.service.BettingService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Spring-injected service references")
public class BetController {

    @GetMapping("/ping")
    public ResponseEntity<Map<String, String>> ping() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    private final BettingService bettingService;
    private final OutboxDispatcher outboxDispatcher;

    public BetController(BettingService bettingService, OutboxDispatcher outboxDispatcher) {
        this.bettingService = bettingService;
        this.outboxDispatcher = outboxDispatcher;
    }

    @PostMapping("/bets")
    public ResponseEntity<PlaceBetResponse> placeBet(
            @Valid @RequestBody PlaceBetRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bettingService.placeBet(request, idempotencyKey));
    }

    @GetMapping("/bets/{betId}")
    public ResponseEntity<BetDetailResponse> getBet(@PathVariable String betId) {
        return bettingService.getBetById(betId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/users/{userId}/bets")
    public ResponseEntity<List<UserBetSummaryResponse>> userBets(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(bettingService.getUserBets(userId, page, size));
    }

    @GetMapping("/events/{eventId}/bets")
    public ResponseEntity<List<UserBetSummaryResponse>> eventBets(
            @PathVariable String eventId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(bettingService.getEventBets(eventId, page, size));
    }

    @PostMapping("/bets/{betId}/cancel")
    public ResponseEntity<CancelBetResponse> cancelBet(@PathVariable String betId) {
        return ResponseEntity.ok(bettingService.cancelBet(betId));
    }

    @PostMapping("/internal/odds")
    public ResponseEntity<Map<String, Boolean>> setOdds(@RequestBody Map<String, Object> payload) {
        String eventId = (String) payload.get("eventId");
        String selection = (String) payload.get("selection");
        Number odds = (Number) payload.get("odds");
        if (eventId == null || selection == null || odds == null) {
            return ResponseEntity.badRequest().build();
        }
        bettingService.setOdds(eventId, selection, BigDecimal.valueOf(odds.doubleValue()));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("accepted", true));
    }

    @GetMapping("/internal/outbox")
    public ResponseEntity<Map<String, List<DomainEvent>>> outbox() {
        return ResponseEntity.ok(Map.of("events", bettingService.outboxEvents()));
    }

    @PostMapping("/internal/outbox/dispatch")
    public ResponseEntity<Map<String, Integer>> dispatchOutbox() {
        return ResponseEntity.ok(Map.of("published", outboxDispatcher.dispatchPending()));
    }
}




