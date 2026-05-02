package com.sportsbetting.apigateway.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

/**
 * Credentials presented by an upstream caller for propagation to downstream HTTP APIs
 * (e.g. {@code Authorization} for JWT or {@code X-API-Key} when odds-service uses API-key auth).
 */
public record DownstreamAuthHeaders(String authorization, String apiKey) {

    public static DownstreamAuthHeaders none() {
        return new DownstreamAuthHeaders(null, null);
    }

    public static DownstreamAuthHeaders from(HttpServletRequest request) {
        if (request == null) {
            return none();
        }
        String auth = blankToNull(request.getHeader(HttpHeaders.AUTHORIZATION));
        String key = blankToNull(request.getHeader("X-API-Key"));
        return new DownstreamAuthHeaders(auth, key);
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
            next = next.header("X-API-Key", apiKey);
        }
        return next;
    }
}
