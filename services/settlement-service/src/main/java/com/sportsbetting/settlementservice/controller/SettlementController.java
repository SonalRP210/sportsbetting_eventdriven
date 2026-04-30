package com.sportsbetting.settlementservice.controller;

import com.sportsbetting.settlementservice.dto.SettleEventRequest;
import com.sportsbetting.settlementservice.dto.SettleEventResponse;
import com.sportsbetting.settlementservice.model.BetPosition;
import com.sportsbetting.settlementservice.model.DomainEvent;
import com.sportsbetting.settlementservice.service.SettlementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class SettlementController {

    private final SettlementService settlementService;

    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @PostMapping("/events/settlements")
    public ResponseEntity<?> settleEvent(@Valid @RequestBody SettleEventRequest request) {
        try {
            SettleEventResponse response = settlementService.settleEvent(request.eventId(), request.winningSelection());
            return ResponseEntity.ok(response);
        } catch (IllegalStateException ex) {
            if ("SETTLEMENT_CONFLICT".equals(ex.getMessage())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "SETTLEMENT_CONFLICT"));
            }
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/internal/open-bets")
    public ResponseEntity<Map<String, Boolean>> seedOpenBet(@RequestBody BetPosition payload) {
        settlementService.seedOpenPosition(payload);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("accepted", true));
    }

    @GetMapping("/internal/positions/{betId}")
    public ResponseEntity<?> getPosition(@PathVariable String betId) {
        return settlementService.getPosition(betId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "bet_not_found")));
    }

    @GetMapping("/internal/outbox")
    public ResponseEntity<Map<String, List<DomainEvent>>> outbox() {
        return ResponseEntity.ok(Map.of("events", settlementService.outboxEvents()));
    }
}
