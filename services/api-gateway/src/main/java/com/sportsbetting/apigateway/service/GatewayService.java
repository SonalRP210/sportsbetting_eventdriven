package com.sportsbetting.apigateway.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Map;

@Service
public class GatewayService {
    private final RestClient restClient;
    private final String applicationName;
    private final String bettingBaseUrl;
    private final String riskBaseUrl;
    private final String walletBaseUrl;
    private final String authBaseUrl;
    private final String userBaseUrl;
    private final String ingestionBaseUrl;
    private final String settlementBaseUrl;
    private final String oddsBaseUrl;

    public GatewayService(
            @Value("${spring.application.name:api-gateway}") String applicationName,
            @Value("${gateway.routes.betting:http://betting-service:8084}") String bettingBaseUrl,
            @Value("${gateway.routes.risk:http://risk-service:8080}") String riskBaseUrl,
            @Value("${gateway.routes.wallet:http://wallet-service:8080}") String walletBaseUrl,
            @Value("${gateway.routes.auth:http://auth-service:8080}") String authBaseUrl,
            @Value("${gateway.routes.user:http://user-service:8080}") String userBaseUrl,
            @Value("${gateway.routes.ingestion:http://event-ingestion-service:8080}") String ingestionBaseUrl,
            @Value("${gateway.routes.settlement:http://settlement-service:8080}") String settlementBaseUrl,
            @Value("${gateway.routes.odds:http://odds-service:8080}") String oddsBaseUrl
    ) {
        this.restClient = RestClient.create();
        this.applicationName = applicationName;
        this.bettingBaseUrl = bettingBaseUrl;
        this.riskBaseUrl = riskBaseUrl;
        this.walletBaseUrl = walletBaseUrl;
        this.authBaseUrl = authBaseUrl;
        this.userBaseUrl = userBaseUrl;
        this.ingestionBaseUrl = ingestionBaseUrl;
        this.settlementBaseUrl = settlementBaseUrl;
        this.oddsBaseUrl = oddsBaseUrl;
    }

    public Map<String, Object> health() {
        return Map.of("gateway", "ok", "version", "v1");
    }

    /** Monolith-compatible shape: {@code GET /api/v1/health}. */
    public Map<String, Object> healthV1() {
        return Map.of(
                "status", "UP",
                "service", applicationName,
                "timestamp", Instant.now().toString()
        );
    }

    public Map<String, Object> routeMap() {
        return Map.of(
                "betting", bettingBaseUrl,
                "risk", riskBaseUrl,
                "wallet", walletBaseUrl,
                "auth", authBaseUrl,
                "user", userBaseUrl,
                "ingestion", ingestionBaseUrl,
                "settlement", settlementBaseUrl,
                "odds", oddsBaseUrl
        );
    }

    public ResponseEntity<String> placeBet(String payload, String idempotencyKey) {
        return forwardWithOptionalHeader(HttpMethod.POST, bettingBaseUrl + "/api/v1/bets", payload, idempotencyKey);
    }

    public ResponseEntity<String> getBet(String betId) {
        return forward(HttpMethod.GET, bettingBaseUrl + "/api/v1/bets/" + betId, null);
    }

    public ResponseEntity<String> cancelBet(String betId) {
        return forward(HttpMethod.POST, bettingBaseUrl + "/api/v1/bets/" + betId + "/cancel", null);
    }

    /** Same JSON contract as monolith: {@code List<OddsUpdate>}. */
    public ResponseEntity<String> oddsFeed(String payload) {
        return forward(HttpMethod.POST, oddsBaseUrl + "/api/v1/odds-feed", payload);
    }

    public ResponseEntity<String> userBets(String userId, int page, int size) {
        return forward(HttpMethod.GET, bettingBaseUrl + "/api/v1/users/" + userId + "/bets?page=" + page + "&size=" + size, null);
    }

    public ResponseEntity<String> eventBets(String eventId, int page, int size) {
        return forward(HttpMethod.GET, bettingBaseUrl + "/api/v1/events/" + eventId + "/bets?page=" + page + "&size=" + size, null);
    }

    public ResponseEntity<String> userExposure(String userId) {
        return forward(HttpMethod.GET, riskBaseUrl + "/api/v1/risk/users/" + userId, null);
    }

    public ResponseEntity<String> totalExposure() {
        return forward(HttpMethod.GET, riskBaseUrl + "/api/v1/risk/total", null);
    }

    public ResponseEntity<String> settleEvent(String payload) {
        return forward(HttpMethod.POST, settlementBaseUrl + "/api/v1/events/settlements", payload);
    }

    public ResponseEntity<String> walletBalance(String userId) {
        return forward(HttpMethod.GET, walletBaseUrl + "/api/v1/wallet/" + userId + "/balance", null);
    }

    public ResponseEntity<String> login(String payload) {
        return forward(HttpMethod.POST, authBaseUrl + "/api/v1/auth/login", payload);
    }

    public ResponseEntity<String> getUser(String userId) {
        return forward(HttpMethod.GET, userBaseUrl + "/api/v1/users/" + userId, null);
    }

    public ResponseEntity<String> upsertUser(String payload) {
        return forward(HttpMethod.POST, userBaseUrl + "/api/v1/users", payload);
    }

    public ResponseEntity<String> ingestProviderEvent(String payload) {
        return forward(HttpMethod.POST, ingestionBaseUrl + "/api/v1/providers/events", payload);
    }

    private ResponseEntity<String> forward(HttpMethod method, String url, String body) {
        try {
            RestClient.RequestBodySpec spec = restClient.method(method).uri(url).header(HttpHeaders.CONTENT_TYPE, "application/json");
            RestClient.ResponseSpec response = (body == null)
                    ? spec.retrieve()
                    : spec.body(body).retrieve();
            String responseBody = response.body(String.class);
            return ResponseEntity.ok(responseBody == null ? "" : responseBody);
        } catch (HttpStatusCodeException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
        }
    }

    private ResponseEntity<String> forwardWithOptionalHeader(HttpMethod method, String url, String body, String idempotencyKey) {
        try {
            RestClient.RequestBodySpec spec = restClient.method(method).uri(url).header(HttpHeaders.CONTENT_TYPE, "application/json");
            if (idempotencyKey != null && !idempotencyKey.isBlank()) {
                spec = spec.header("Idempotency-Key", idempotencyKey);
            }
            String responseBody = spec.body(body).retrieve().body(String.class);
            return ResponseEntity.status(HttpStatusCode.valueOf(201)).body(responseBody == null ? "" : responseBody);
        } catch (HttpStatusCodeException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
        }
    }
}
