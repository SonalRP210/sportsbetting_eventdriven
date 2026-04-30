package com.sportsbetting.notificationservice.dto;

import java.util.Map;

public record NotificationEventRequest(String eventType, String userId, Map<String, Object> data) {}
