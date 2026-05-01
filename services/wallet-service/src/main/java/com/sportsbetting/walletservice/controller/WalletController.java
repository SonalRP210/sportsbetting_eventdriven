package com.sportsbetting.walletservice.controller;

import com.sportsbetting.walletservice.dto.SettlementEventRequest;
import com.sportsbetting.walletservice.dto.WalletBalanceResponse;
import com.sportsbetting.walletservice.dto.WalletTransactionRequest;
import com.sportsbetting.walletservice.model.DomainEvent;
import com.sportsbetting.walletservice.model.WalletTransaction;
import com.sportsbetting.walletservice.service.OutboxDispatcher;
import com.sportsbetting.walletservice.service.WalletService;
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
public class WalletController {

    private final WalletService walletService;
    private final OutboxDispatcher outboxDispatcher;

    public WalletController(WalletService walletService, OutboxDispatcher outboxDispatcher) {
        this.walletService = walletService;
        this.outboxDispatcher = outboxDispatcher;
    }

    @PostMapping("/wallet/transactions")
    public ResponseEntity<?> postTransaction(@Valid @RequestBody WalletTransactionRequest request) {
        try {
            WalletTransaction tx = walletService.postTransaction(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(tx);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/wallet/{userId}/balance")
    public ResponseEntity<WalletBalanceResponse> getBalance(@PathVariable String userId) {
        return ResponseEntity.ok(walletService.getBalance(userId));
    }

    @PostMapping("/internal/settlement-events")
    public ResponseEntity<Map<String, Object>> settlementEvent(@Valid @RequestBody SettlementEventRequest event) {
        int posted = walletService.consumeSettlementEvent(event);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("accepted", true, "posted", posted));
    }

    @GetMapping("/internal/outbox")
    public ResponseEntity<Map<String, List<DomainEvent>>> outbox() {
        return ResponseEntity.ok(Map.of("events", walletService.outboxEvents()));
    }

    @PostMapping("/internal/outbox/dispatch")
    public ResponseEntity<Map<String, Object>> dispatchOutbox() {
        int sent = outboxDispatcher.dispatchPending();
        return ResponseEntity.ok(Map.of("dispatched", sent));
    }
}
