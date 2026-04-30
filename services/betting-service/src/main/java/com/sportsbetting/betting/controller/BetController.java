package com.sportsbetting.betting.controller;

import com.sportsbetting.betting.dto.BetDetailResponse;
import com.sportsbetting.betting.dto.CancelBetResponse;
import com.sportsbetting.betting.dto.PlaceBetRequest;
import com.sportsbetting.betting.dto.PlaceBetResponse;
import com.sportsbetting.betting.model.DomainEvent;
import com.sportsbetting.betting.service.BettingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class BetController {

    private final BettingService bettingService;

    public BetController(BettingService bettingService) {
        this.bettingService = bettingService;
    }

    @PostMapping("/bets")
    public ResponseEntity<PlaceBetResponse> placeBet(
            @Valid @RequestBody PlaceBetRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(bettingService.placeBet(request, idempotencyKey));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/bets/{betId}")
    public ResponseEntity<BetDetailResponse> getBet(@PathVariable String betId) {
        return bettingService.getBetById(betId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/bets/{betId}/cancel")
    public ResponseEntity<CancelBetResponse> cancelBet(@PathVariable String betId) {
        try {
            return ResponseEntity.ok(bettingService.cancelBet(betId));
        } catch (IllegalArgumentException ex) {
            if ("Bet not found".equals(ex.getMessage())) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().build();
        }
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

    @GetMapping("/ping")
    public ResponseEntity<Map<String, String>> ping() {
        return ResponseEntity.ok(Map.of("message", "betting-service pong"));
    }
}
