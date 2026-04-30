package com.sportsbetting.authservice.service;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AuthService {
 public Map<String,Object> login(String username, String password){
  if(username == null || password == null || password.isBlank()) throw new IllegalArgumentException("INVALID_CREDENTIALS");
  return Map.of("accessToken", "token-" + username, "tokenType", "Bearer", "expiresIn", 3600);
 }
}
