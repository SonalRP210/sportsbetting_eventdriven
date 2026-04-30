package com.sportsbetting.notificationservice.service;

import com.sportsbetting.notificationservice.dto.NotificationEventRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class NotificationService {
  private final List<Map<String,Object>> deliveries = new ArrayList<>();
  public void consume(NotificationEventRequest req){
    deliveries.add(Map.of("eventType", req.eventType(), "userId", req.userId(), "status", "SENT", "sentAt", Instant.now().toString()));
  }
  public List<Map<String,Object>> deliveries(){ return List.copyOf(deliveries); }
  public void resetForTests(){ deliveries.clear(); }
}
