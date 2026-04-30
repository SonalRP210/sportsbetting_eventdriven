package com.sportsbetting.apigateway.controller;

import com.sportsbetting.apigateway.service.GatewayService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class GatewayController {
 private final GatewayService service;
 public GatewayController(GatewayService service){ this.service = service; }
 @GetMapping("/gateway/health")
 public ResponseEntity<?> health(){ return ResponseEntity.ok(service.health()); }
 @GetMapping("/gateway/routes")
 public ResponseEntity<?> routes(){ return ResponseEntity.ok(service.routeMap()); }
}
