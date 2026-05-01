package com.sportsbetting.riskservice.controller;

import com.sportsbetting.riskservice.dto.*;
import com.sportsbetting.riskservice.model.DomainEvent;
import com.sportsbetting.riskservice.service.OutboxDispatcher;
import com.sportsbetting.riskservice.service.RiskService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class RiskController {
    private final RiskService riskService;
    private final OutboxDispatcher outboxDispatcher;

    public RiskController(RiskService riskService, OutboxDispatcher outboxDispatcher) {
        this.riskService = riskService;
        this.outboxDispatcher = outboxDispatcher;
    }

    @PostMapping("/internal/events/bet-placed")
    public ResponseEntity<Map<String, Boolean>> onBetPlaced(@RequestBody BetPlacedEventRequest request) {
        riskService.onBetPlaced(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("accepted", true));
    }

    @PostMapping("/internal/events/bet-cancelled")
    public ResponseEntity<Map<String, Boolean>> onBetCancelled(@RequestBody BetCancelledEventRequest request) {
        riskService.onBetCancelled(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("accepted", true));
    }

    @PostMapping("/internal/events/event-settled")
    public ResponseEntity<Map<String, Boolean>> onEventSettled(@RequestBody EventSettledRequest request) {
        riskService.onEventSettled(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("accepted", true));
    }

    @GetMapping("/risk/users/{userId}")
    public ResponseEntity<UserExposureResponse> getUserExposure(@PathVariable String userId) {
        return ResponseEntity.ok(riskService.getUserExposure(userId));
    }

    @GetMapping("/risk/total")
    public ResponseEntity<Map<String, BigDecimal>> getTotalExposure() {
        return ResponseEntity.ok(Map.of("totalExposure", riskService.getTotalExposure()));
    }

    @GetMapping("/internal/outbox")
    public ResponseEntity<Map<String, List<DomainEvent>>> outbox() {
        return ResponseEntity.ok(Map.of("events", riskService.outboxEvents()));
    }

    @PostMapping("/internal/outbox/dispatch")
    public ResponseEntity<Map<String, Object>> dispatchOutbox() {
        return ResponseEntity.ok(Map.of("dispatched", outboxDispatcher.dispatchPending()));
    }
}
