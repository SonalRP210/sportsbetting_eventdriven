package com.sportsbetting.eventingestionservice.controller;

import com.sportsbetting.eventingestionservice.dto.ProviderEventRequest;
import com.sportsbetting.eventingestionservice.service.IngestionService;
import com.sportsbetting.eventingestionservice.service.OutboxDispatcher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class IngestionController {
  private final IngestionService service;
  private final OutboxDispatcher outboxDispatcher;

  public IngestionController(IngestionService service, OutboxDispatcher outboxDispatcher){
    this.service = service;
    this.outboxDispatcher = outboxDispatcher;
  }

  @PostMapping("/providers/events")
  public ResponseEntity<Map<String,Boolean>> ingest(@RequestBody ProviderEventRequest req){
    service.ingest(req);
    return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("accepted", true));
  }

  @GetMapping("/internal/normalized-events")
  public ResponseEntity<?> events(){
    return ResponseEntity.ok(Map.of("events", service.events()));
  }

  @PostMapping("/internal/outbox/dispatch")
  public ResponseEntity<Map<String, Object>> dispatchOutbox() {
    return ResponseEntity.ok(Map.of("dispatched", outboxDispatcher.dispatchPending()));
  }
}
