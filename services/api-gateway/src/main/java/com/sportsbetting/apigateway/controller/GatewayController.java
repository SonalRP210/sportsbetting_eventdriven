package com.sportsbetting.apigateway.controller;

import com.sportsbetting.apigateway.service.DownstreamAuthHeaders;
import com.sportsbetting.apigateway.service.GatewayService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Spring-injected singleton service reference")
public class GatewayController {

    private final GatewayService service;
    private final String downstreamApiKeyHeader;

    public GatewayController(
            GatewayService service,
            @Value("${gateway.forward.api-key-header:X-API-Key}") String downstreamApiKeyHeader
    ) {
        this.service = service;
        this.downstreamApiKeyHeader = downstreamApiKeyHeader;
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
    public ResponseEntity<String> getUser(@PathVariable String userId, HttpServletRequest request) {
        return service.getUser(userId, DownstreamAuthHeaders.from(request, downstreamApiKeyHeader));
    }

    @PostMapping("/users")
    public ResponseEntity<String> upsertUser(@RequestBody String payload, HttpServletRequest request) {
        return service.upsertUser(payload, DownstreamAuthHeaders.from(request, downstreamApiKeyHeader));
    }

    @PostMapping("/providers/events")
    public ResponseEntity<String> ingestProviderEvent(@RequestBody String payload, HttpServletRequest request) {
        return service.ingestProviderEvent(payload, DownstreamAuthHeaders.from(request, downstreamApiKeyHeader));
    }

    @PostMapping("/bets")
    public ResponseEntity<String> placeBet(
            @RequestBody String payload,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            HttpServletRequest request
    ) {
        return service.placeBet(payload, idempotencyKey, DownstreamAuthHeaders.from(request, downstreamApiKeyHeader));
    }

    @GetMapping("/bets/{betId}")
    public ResponseEntity<String> getBet(@PathVariable String betId, HttpServletRequest request) {
        return service.getBet(betId, DownstreamAuthHeaders.from(request, downstreamApiKeyHeader));
    }

    @PostMapping("/bets/{betId}/cancel")
    public ResponseEntity<String> cancelBet(@PathVariable String betId, HttpServletRequest request) {
        return service.cancelBet(betId, DownstreamAuthHeaders.from(request, downstreamApiKeyHeader));
    }

    @PostMapping("/odds-feed")
    public ResponseEntity<String> oddsFeed(@RequestBody String payload, HttpServletRequest request) {
        return service.oddsFeed(payload, DownstreamAuthHeaders.from(request, downstreamApiKeyHeader));
    }

    @PostMapping("/events/settlements")
    public ResponseEntity<String> settle(@RequestBody String payload, HttpServletRequest request) {
        return service.settleEvent(payload, DownstreamAuthHeaders.from(request, downstreamApiKeyHeader));
    }

    @GetMapping("/users/{userId}/bets")
    public ResponseEntity<String> userBets(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request
    ) {
        return service.userBets(userId, page, size, DownstreamAuthHeaders.from(request, downstreamApiKeyHeader));
    }

    @GetMapping("/events/{eventId}/bets")
    public ResponseEntity<String> eventBets(
            @PathVariable String eventId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request
    ) {
        return service.eventBets(eventId, page, size, DownstreamAuthHeaders.from(request, downstreamApiKeyHeader));
    }

    @GetMapping("/users/{userId}/exposure")
    public ResponseEntity<String> userExposure(@PathVariable String userId, HttpServletRequest request) {
        return service.userExposure(userId, DownstreamAuthHeaders.from(request, downstreamApiKeyHeader));
    }

    @GetMapping("/risk/total")
    public ResponseEntity<String> totalExposure(HttpServletRequest request) {
        return service.totalExposure(DownstreamAuthHeaders.from(request, downstreamApiKeyHeader));
    }

    @GetMapping("/wallet/{userId}/balance")
    public ResponseEntity<String> walletBalance(@PathVariable String userId, HttpServletRequest request) {
        return service.walletBalance(userId, DownstreamAuthHeaders.from(request, downstreamApiKeyHeader));
    }
}
