package com.sportsbetting.oddsservice.controller;

import com.sportsbetting.oddsservice.api.dto.OddsFeedAcceptedResponse;
import com.sportsbetting.oddsservice.api.dto.OddsLookupResponse;
import com.sportsbetting.oddsservice.api.dto.OddsNotFoundResponse;
import com.sportsbetting.oddsservice.api.dto.OddsQuoteResponse;
import com.sportsbetting.oddsservice.api.dto.OutboxDispatchResponse;
import com.sportsbetting.oddsservice.api.dto.OutboxEventsResponse;
import com.sportsbetting.oddsservice.model.odds.OddsUpdate;
import com.sportsbetting.oddsservice.outbox.OutboxDispatcher;
import com.sportsbetting.oddsservice.service.OddsService;
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

@RestController
@RequestMapping("/api/v1")
public class OddsController {

    private final OddsService oddsService;
    private final OutboxDispatcher outboxDispatcher;

    public OddsController(OddsService oddsService, OutboxDispatcher outboxDispatcher) {
        this.oddsService = oddsService;
        this.outboxDispatcher = outboxDispatcher;
    }

    /**
     * Validates the batch, then persists in bounded DB chunks ({@code app.odds.feed.chunk-size}).
     * {@code 202 ACCEPTED} means writes were scheduled successfully — not that every downstream consumer
     * or bet-placement path has observed the price; correlate via {@code updatedAt} on reads when you need RYW semantics.
     */
    @PostMapping("/odds-feed")
    public ResponseEntity<OddsFeedAcceptedResponse> oddsFeed(@RequestBody @Valid List<OddsUpdate> updates) {
        oddsService.consumeOddsFeed(updates);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(OddsFeedAcceptedResponse.accepted());
    }

    @GetMapping("/odds/{eventId}/{selection}")
    public ResponseEntity<OddsLookupResponse> getOdds(
            @PathVariable String eventId,
            @PathVariable String selection
    ) {
        return oddsService.getOdds(eventId, selection)
                .<ResponseEntity<OddsLookupResponse>>map(odds ->
                        ResponseEntity.ok(new OddsQuoteResponse(eventId, selection, odds)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(OddsNotFoundResponse.oddsNotFound()));
    }

    @GetMapping("/internal/outbox")
    public ResponseEntity<OutboxEventsResponse> outbox() {
        return ResponseEntity.ok(new OutboxEventsResponse(oddsService.outboxEvents()));
    }

    @PostMapping("/internal/outbox/dispatch")
    public ResponseEntity<OutboxDispatchResponse> dispatchOutbox() {
        int sent = outboxDispatcher.dispatchPending();
        return ResponseEntity.ok(new OutboxDispatchResponse(sent));
    }
}
