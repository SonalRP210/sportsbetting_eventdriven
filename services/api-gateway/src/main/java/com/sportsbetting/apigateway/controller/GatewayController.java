package com.sportsbetting.apigateway.controller;

import com.sportsbetting.apigateway.service.GatewayService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class GatewayController {

    private final GatewayService service;

    public GatewayController(GatewayService service) {
        this.service = service;
    }

    /** Same path as monolith {@code AppConstants.Api.HEALTH}. */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthV1() {
        return ResponseEntity.ok(service.healthV1());
    }

    @GetMapping("/gateway/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(service.health());
    }

    @GetMapping("/gateway/routes")
    public ResponseEntity<Map<String, Object>> routes() {
        return ResponseEntity.ok(service.routeMap());
    }

    @PostMapping("/auth/login")
    public ResponseEntity<String> login(@RequestBody String payload) {
        return service.login(payload);
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<String> getUser(@PathVariable String userId) {
        return service.getUser(userId);
    }

    @PostMapping("/users")
    public ResponseEntity<String> upsertUser(@RequestBody String payload) {
        return service.upsertUser(payload);
    }

    @PostMapping("/providers/events")
    public ResponseEntity<String> ingestProviderEvent(@RequestBody String payload) {
        return service.ingestProviderEvent(payload);
    }

    @PostMapping("/bets")
    public ResponseEntity<String> placeBet(
            @RequestBody String payload,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        return service.placeBet(payload, idempotencyKey);
    }

    @GetMapping("/bets/{betId}")
    public ResponseEntity<String> getBet(@PathVariable String betId) {
        return service.getBet(betId);
    }

    @PostMapping("/bets/{betId}/cancel")
    public ResponseEntity<String> cancelBet(@PathVariable String betId) {
        return service.cancelBet(betId);
    }

    @PostMapping("/odds-feed")
    public ResponseEntity<String> oddsFeed(@RequestBody String payload) {
        return service.oddsFeed(payload);
    }

    @PostMapping("/events/settlements")
    public ResponseEntity<String> settle(@RequestBody String payload) {
        return service.settleEvent(payload);
    }

    @GetMapping("/users/{userId}/bets")
    public ResponseEntity<String> userBets(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return service.userBets(userId, page, size);
    }

    @GetMapping("/events/{eventId}/bets")
    public ResponseEntity<String> eventBets(
            @PathVariable String eventId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return service.eventBets(eventId, page, size);
    }

    @GetMapping("/users/{userId}/exposure")
    public ResponseEntity<String> userExposure(@PathVariable String userId) {
        return service.userExposure(userId);
    }

    @GetMapping("/risk/total")
    public ResponseEntity<String> totalExposure() {
        return service.totalExposure();
    }

    @GetMapping("/wallet/{userId}/balance")
    public ResponseEntity<String> walletBalance(@PathVariable String userId) {
        return service.walletBalance(userId);
    }
}
