package com.sportsbetting.eventingestionservice.service;

import com.sportsbetting.eventingestionservice.dto.ProviderEventRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class IngestionService {
  private final List<Map<String,Object>> normalizedEvents = new ArrayList<>();
  public void ingest(ProviderEventRequest req){ normalizedEvents.add(Map.of("provider", req.provider(), "eventType", req.eventType(), "normalizedAt", Instant.now().toString(), "payload", req.payload())); }
  public List<Map<String,Object>> events(){ return List.copyOf(normalizedEvents); }
  public void resetForTests(){ normalizedEvents.clear(); }
}
