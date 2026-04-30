package com.sportsbetting.notificationservice.controller;

import com.sportsbetting.notificationservice.dto.NotificationEventRequest;
import com.sportsbetting.notificationservice.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class NotificationController {
  private final NotificationService service;
  public NotificationController(NotificationService service){ this.service = service; }
  @PostMapping("/internal/events")
  public ResponseEntity<Map<String,Boolean>> consume(@RequestBody NotificationEventRequest req){ service.consume(req); return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("accepted", true)); }
  @GetMapping("/internal/deliveries")
  public ResponseEntity<?> deliveries(){ return ResponseEntity.ok(Map.of("deliveries", service.deliveries())); }
}
