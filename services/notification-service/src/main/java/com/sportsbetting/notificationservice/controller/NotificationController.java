package com.sportsbetting.notificationservice.controller;

import com.sportsbetting.notificationservice.dto.NotificationEventRequest;
import com.sportsbetting.notificationservice.service.NotificationService;
import com.sportsbetting.notificationservice.service.OutboxDispatcher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class NotificationController {
  private final NotificationService service;
  private final OutboxDispatcher outboxDispatcher;

  public NotificationController(NotificationService service, OutboxDispatcher outboxDispatcher){
    this.service = service;
    this.outboxDispatcher = outboxDispatcher;
  }

  @PostMapping("/internal/events")
  public ResponseEntity<Map<String,Boolean>> consume(@RequestBody NotificationEventRequest req){
    service.consume(req);
    return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("accepted", true));
  }

  @GetMapping("/internal/deliveries")
  public ResponseEntity<?> deliveries(){
    return ResponseEntity.ok(Map.of("deliveries", service.deliveries()));
  }

  @PostMapping("/internal/outbox/dispatch")
  public ResponseEntity<Map<String, Object>> dispatchOutbox() {
    return ResponseEntity.ok(Map.of("dispatched", outboxDispatcher.dispatchPending()));
  }
}
