package com.sportsbetting.apigateway.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

/**
 * Credentials presented by an upstream caller for propagation to downstream HTTP APIs
 * (e.g. {@code Authorization} for JWT or a configurable API-key header aligned with odds-service).
 */
public record DownstreamAuthHeaders(String authorization, String apiKey, String outboundApiKeyHeader) {

    public DownstreamAuthHeaders {
        if (outboundApiKeyHeader == null || outboundApiKeyHeader.isBlank()) {
            outboundApiKeyHeader = "X-API-Key";
        }
    }

    public static DownstreamAuthHeaders none() {
        return new DownstreamAuthHeaders(null, null, "X-API-Key");
    }

    /**
     * @param apiKeyHeaderName header used inbound and outbound (match {@code app.security.api-key-header} on odds-service).
     */
    public static DownstreamAuthHeaders from(HttpServletRequest request, String apiKeyHeaderName) {
        if (request == null) {
            return none();
        }
        String auth = blankToNull(request.getHeader(HttpHeaders.AUTHORIZATION));
        String resolvedHeader = apiKeyHeaderName != null && !apiKeyHeaderName.isBlank() ? apiKeyHeaderName : "X-API-Key";
        String key = blankToNull(request.getHeader(resolvedHeader));
        return new DownstreamAuthHeaders(auth, key, resolvedHeader);
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    public <T extends RestClient.RequestHeadersSpec<T>> T apply(T spec) {
        T next = spec;
        if (authorization != null) {
            next = next.header(HttpHeaders.AUTHORIZATION, authorization);
        }
        if (apiKey != null) {
            next = next.header(outboundApiKeyHeader, apiKey);
        }
        return next;
    }
}
