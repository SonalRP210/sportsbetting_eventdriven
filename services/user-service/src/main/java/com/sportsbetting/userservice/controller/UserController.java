package com.sportsbetting.userservice.controller;

import com.sportsbetting.userservice.model.UserProfile;
import com.sportsbetting.userservice.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class UserController {
 private final UserService service;
 public UserController(UserService service){ this.service = service; }
 @GetMapping("/users/{userId}")
 public ResponseEntity<?> get(@PathVariable String userId){ return service.get(userId).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build()); }
 @PostMapping("/users")
 public ResponseEntity<UserProfile> upsert(@RequestBody UserProfile profile){ return ResponseEntity.ok(service.upsert(profile)); }
}
