package com.sportsbetting.apigateway.service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Map;

@Service
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Spring-injected RestClient singleton")
public class GatewayService {
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String applicationName;
    private final String oidcTokenEndpoint;
    private final String oidcClientId;
    private final String bettingBaseUrl;
    private final String riskBaseUrl;
    private final String walletBaseUrl;
    private final String userBaseUrl;
    private final String ingestionBaseUrl;
    private final String settlementBaseUrl;
    private final String oddsBaseUrl;

    public GatewayService(
            RestClient gatewayRestClient,
            ObjectMapper objectMapper,
            @Value("${spring.application.name:api-gateway}") String applicationName,
            @Value("${gateway.oidc.token-endpoint:}") String oidcTokenEndpoint,
            @Value("${gateway.oidc.client-id:odds-gateway}") String oidcClientId,
            @Value("${gateway.routes.betting:http://betting-service:8084}") String bettingBaseUrl,
            @Value("${gateway.routes.risk:http://risk-service:8080}") String riskBaseUrl,
            @Value("${gateway.routes.wallet:http://wallet-service:8080}") String walletBaseUrl,
            @Value("${gateway.routes.user:http://user-service:8080}") String userBaseUrl,
            @Value("${gateway.routes.ingestion:http://event-ingestion-service:8080}") String ingestionBaseUrl,
            @Value("${gateway.routes.settlement:http://settlement-service:8080}") String settlementBaseUrl,
            @Value("${gateway.routes.odds:http://odds-service:8080}") String oddsBaseUrl
    ) {
        this.restClient = gatewayRestClient;
        this.objectMapper = objectMapper;
        this.applicationName = applicationName;
        this.oidcTokenEndpoint = oidcTokenEndpoint;
        this.oidcClientId = oidcClientId;
        this.bettingBaseUrl = bettingBaseUrl;
        this.riskBaseUrl = riskBaseUrl;
        this.walletBaseUrl = walletBaseUrl;
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
                "user", userBaseUrl,
                "ingestion", ingestionBaseUrl,
                "settlement", settlementBaseUrl,
                "odds", oddsBaseUrl
        );
    }

    public ResponseEntity<String> placeBet(String payload, String idempotencyKey, DownstreamAuthHeaders downstreamAuth) {
        return forwardWithOptionalHeader(
                HttpMethod.POST, bettingBaseUrl + "/api/v1/bets", payload, idempotencyKey, downstreamAuth);
    }

    public ResponseEntity<String> getBet(String betId, DownstreamAuthHeaders downstreamAuth) {
        return forward(HttpMethod.GET, bettingBaseUrl + "/api/v1/bets/" + betId, null, downstreamAuth);
    }

    public ResponseEntity<String> cancelBet(String betId, DownstreamAuthHeaders downstreamAuth) {
        return forward(HttpMethod.POST, bettingBaseUrl + "/api/v1/bets/" + betId + "/cancel", null, downstreamAuth);
    }

    /** Same JSON contract as monolith: {@code List<OddsUpdate>}. */
    public ResponseEntity<String> oddsFeed(String payload, DownstreamAuthHeaders downstreamAuth) {
        return forward(HttpMethod.POST, oddsBaseUrl + "/api/v1/odds-feed", payload, downstreamAuth);
    }

    public ResponseEntity<String> userBets(String userId, int page, int size, DownstreamAuthHeaders downstreamAuth) {
        return forward(
                HttpMethod.GET,
                bettingBaseUrl + "/api/v1/users/" + userId + "/bets?page=" + page + "&size=" + size,
                null,
                downstreamAuth);
    }

    public ResponseEntity<String> eventBets(String eventId, int page, int size, DownstreamAuthHeaders downstreamAuth) {
        return forward(
                HttpMethod.GET,
                bettingBaseUrl + "/api/v1/events/" + eventId + "/bets?page=" + page + "&size=" + size,
                null,
                downstreamAuth);
    }

    public ResponseEntity<String> userExposure(String userId, DownstreamAuthHeaders downstreamAuth) {
        return forward(HttpMethod.GET, riskBaseUrl + "/api/v1/risk/users/" + userId, null, downstreamAuth);
    }

    public ResponseEntity<String> totalExposure(DownstreamAuthHeaders downstreamAuth) {
        return forward(HttpMethod.GET, riskBaseUrl + "/api/v1/risk/total", null, downstreamAuth);
    }

    public ResponseEntity<String> settleEvent(String payload, DownstreamAuthHeaders downstreamAuth) {
        return forward(HttpMethod.POST, settlementBaseUrl + "/api/v1/events/settlements", payload, downstreamAuth);
    }

    public ResponseEntity<String> walletBalance(String userId, DownstreamAuthHeaders downstreamAuth) {
        return forward(HttpMethod.GET, walletBaseUrl + "/api/v1/wallet/" + userId + "/balance", null, downstreamAuth);
    }

    /**
     * When {@code gateway.oidc.token-endpoint} is unset, returns {@code 501}.
     * When set (typically Keycloak in Docker), performs an OAuth 2 Password Grant (RFC legacy;
     * use Authorization Code flow in production browsers) and returns the IdP token JSON verbatim.
     */
    public ResponseEntity<String> login(String payload) {
        if (oidcTokenEndpoint == null || oidcTokenEndpoint.isBlank()) {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                    .header(HttpHeaders.CONTENT_TYPE, "application/json")
                    .body("{\"error\":\"AUTH_NOT_IMPLEMENTED\",\"message\":\"Configure gateway.oidc.token-endpoint (e.g. Keycloak token URL).\"}");
        }

        JsonNode tree;
        try {
            tree = objectMapper.readTree(payload == null || payload.isBlank() ? "{}" : payload);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body("{\"error\":\"INVALID_LOGIN_JSON\"}");
        }
        String username = textOrNull(tree, "username");
        String password = textOrNull(tree, "password");
        if (username == null || password == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body("{\"error\":\"USERNAME_PASSWORD_REQUIRED\"}");
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", oidcClientId);
        form.add("username", username);
        form.add("password", password);

        try {
            String body = restClient.post()
                    .uri(oidcTokenEndpoint)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(String.class);
            return ResponseEntity.status(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(body == null ? "" : body);
        } catch (HttpStatusCodeException ex) {
            return ResponseEntity.status(ex.getStatusCode())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(ex.getResponseBodyAsString());
        }
    }

    private static String textOrNull(JsonNode tree, String field) {
        if (!tree.hasNonNull(field)) {
            return null;
        }
        String value = tree.get(field).asText();
        return value == null || value.isBlank() ? null : value;
    }

    public ResponseEntity<String> getUser(String userId, DownstreamAuthHeaders downstreamAuth) {
        return forward(HttpMethod.GET, userBaseUrl + "/api/v1/users/" + userId, null, downstreamAuth);
    }

    public ResponseEntity<String> upsertUser(String payload, DownstreamAuthHeaders downstreamAuth) {
        return forward(HttpMethod.POST, userBaseUrl + "/api/v1/users", payload, downstreamAuth);
    }

    public ResponseEntity<String> ingestProviderEvent(String payload, DownstreamAuthHeaders downstreamAuth) {
        return forward(HttpMethod.POST, ingestionBaseUrl + "/api/v1/providers/events", payload, downstreamAuth);
    }

    private ResponseEntity<String> forward(HttpMethod method, String url, String body, DownstreamAuthHeaders downstreamAuth) {
        try {
            RestClient.RequestBodySpec spec = downstreamAuth.apply(
                    restClient.method(method).uri(url).header(HttpHeaders.CONTENT_TYPE, "application/json"));
            RestClient.ResponseSpec response = (body == null)
                    ? spec.retrieve()
                    : spec.body(body).retrieve();
            String responseBody = response.body(String.class);
            return ResponseEntity.ok(responseBody == null ? "" : responseBody);
        } catch (HttpStatusCodeException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
        }
    }

    private ResponseEntity<String> forwardWithOptionalHeader(
            HttpMethod method,
            String url,
            String body,
            String idempotencyKey,
            DownstreamAuthHeaders downstreamAuth
    ) {
        try {
            RestClient.RequestBodySpec spec = downstreamAuth.apply(
                    restClient.method(method).uri(url).header(HttpHeaders.CONTENT_TYPE, "application/json"));
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
