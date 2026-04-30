package com.sportsbetting.oddsservice.controller;

import com.sportsbetting.oddsservice.model.DomainEvent;
import com.sportsbetting.oddsservice.model.OddsUpdate;
import com.sportsbetting.oddsservice.service.OddsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class OddsController {

    private final OddsService oddsService;

    public OddsController(OddsService oddsService) {
        this.oddsService = oddsService;
    }

    @PostMapping("/odds-feed")
    public ResponseEntity<Map<String, String>> oddsFeed(@RequestBody List<OddsUpdate> updates) {
        try {
            oddsService.consumeOddsFeed(updates);
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("message", "Odds feed accepted"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/odds/{eventId}/{selection}")
    public ResponseEntity<Map<String, Object>> getOdds(@PathVariable String eventId, @PathVariable String selection) {
        return oddsService.getOdds(eventId, selection)
                .<ResponseEntity<Map<String, Object>>>map(odds -> ResponseEntity.ok(Map.of(
                        "eventId", eventId,
                        "selection", selection,
                        "odds", odds
                )))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "odds_not_found")));
    }

    @GetMapping("/internal/outbox")
    public ResponseEntity<Map<String, List<DomainEvent>>> outbox() {
        return ResponseEntity.ok(Map.of("events", oddsService.outboxEvents()));
    }

    @PostMapping("/internal/seed-odds")
    public ResponseEntity<Map<String, Boolean>> seedOdds(@RequestBody Map<String, Object> payload) {
        String eventId = (String) payload.get("eventId");
        String selection = (String) payload.get("selection");
        Number odds = (Number) payload.get("odds");
        if (eventId == null || selection == null || odds == null) {
            return ResponseEntity.badRequest().build();
        }
        oddsService.consumeOddsFeed(List.of(new OddsUpdate(eventId, selection, BigDecimal.valueOf(odds.doubleValue()))));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("accepted", true));
    }
}
