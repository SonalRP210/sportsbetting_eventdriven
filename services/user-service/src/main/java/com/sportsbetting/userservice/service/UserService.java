package com.sportsbetting.userservice.service;

import com.sportsbetting.userservice.model.UserProfile;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserService {
 private final Map<String, UserProfile> users = new ConcurrentHashMap<>();
 public UserService(){ users.put("user-1", new UserProfile("user-1", "user1@example.com", "ACTIVE")); }
 public Optional<UserProfile> get(String userId){ return Optional.ofNullable(users.get(userId)); }
 public UserProfile upsert(UserProfile profile){ users.put(profile.userId(), profile); return profile; }
}
