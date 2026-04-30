package com.sportsbetting.apigateway.service;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class GatewayService {
 public Map<String,Object> health(){ return Map.of("gateway","ok","version","v1"); }
 public Map<String,Object> routeMap(){ return Map.of(
  "betting", "http://betting-service:8084",
  "odds", "http://odds-service:8085",
  "wallet", "http://wallet-service:8083",
  "risk", "http://risk-service:8089"
 ); }
}
