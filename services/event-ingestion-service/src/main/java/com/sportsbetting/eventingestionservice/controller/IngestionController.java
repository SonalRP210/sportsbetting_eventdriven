package com.sportsbetting.eventingestionservice.controller;

import com.sportsbetting.eventingestionservice.dto.ProviderEventRequest;
import com.sportsbetting.eventingestionservice.service.IngestionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class IngestionController {
  private final IngestionService service;
  public IngestionController(IngestionService service){ this.service = service; }
  @PostMapping("/providers/events")
  public ResponseEntity<Map<String,Boolean>> ingest(@RequestBody ProviderEventRequest req){ service.ingest(req); return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("accepted", true)); }
  @GetMapping("/internal/normalized-events")
  public ResponseEntity<?> events(){ return ResponseEntity.ok(Map.of("events", service.events())); }
}
