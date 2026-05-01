package com.sportsbetting.oddsservice.controller;

import com.sportsbetting.oddsservice.model.DomainEvent;
import com.sportsbetting.oddsservice.model.OddsUpdate;
import com.sportsbetting.oddsservice.service.OddsService;
import com.sportsbetting.oddsservice.service.OutboxDispatcher;
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
public class OddsController {

    private final OddsService oddsService;
    private final OutboxDispatcher outboxDispatcher;

    public OddsController(OddsService oddsService, OutboxDispatcher outboxDispatcher) {
        this.oddsService = oddsService;
        this.outboxDispatcher = outboxDispatcher;
    }

    @PostMapping("/odds-feed")
    public ResponseEntity<Map<String, String>> oddsFeed(@RequestBody @Valid List<OddsUpdate> updates) {
        oddsService.consumeOddsFeed(updates);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("message", "Odds feed accepted"));
    }

    @GetMapping("/odds/{eventId}/{selection}")
    public ResponseEntity<Map<String, Object>> getOdds(
            @PathVariable String eventId,
            @PathVariable String selection
    ) {
        return oddsService.getOdds(eventId, selection)
                .<ResponseEntity<Map<String, Object>>>map(odds -> ResponseEntity.ok(Map.of(
                        "eventId", eventId,
                        "selection", selection,
                        "odds", odds
                )))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "odds_not_found")));
    }

    @GetMapping("/internal/outbox")
    public ResponseEntity<Map<String, List<DomainEvent>>> outbox() {
        return ResponseEntity.ok(Map.of("events", oddsService.outboxEvents()));
    }

    @PostMapping("/internal/outbox/dispatch")
    public ResponseEntity<Map<String, Object>> dispatchOutbox() {
        int sent = outboxDispatcher.dispatchPending();
        return ResponseEntity.ok(Map.of("dispatched", sent));
    }

    /**
     * Test/local seed helper. Accepts the same shape as /odds-feed for a single update.
     * Protected by the internal path prefix — do not expose via the API gateway in production.
     */
    @PostMapping("/internal/seed-odds")
    public ResponseEntity<Map<String, Boolean>> seedOdds(@RequestBody @Valid OddsUpdate request) {
        oddsService.consumeOddsFeed(List.of(request));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("accepted", true));
    }
}
