package com.sportsbetting.eventingestionservice.dto;

import java.util.Map;

public record ProviderEventRequest(String provider, String eventType, Map<String, Object> payload) {}
