package com.sportsbetting.authservice.controller;

import com.sportsbetting.authservice.dto.LoginRequest;
import com.sportsbetting.authservice.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class AuthController {
 private final AuthService service;
 public AuthController(AuthService service){ this.service = service; }
 @PostMapping("/auth/login")
 public ResponseEntity<?> login(@RequestBody LoginRequest req){
  try { return ResponseEntity.ok(service.login(req.username(), req.password())); }
  catch (IllegalArgumentException ex){ return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage())); }
 }
}
