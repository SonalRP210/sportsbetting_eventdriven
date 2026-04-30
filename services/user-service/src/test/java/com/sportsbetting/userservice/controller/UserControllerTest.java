package com.sportsbetting.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {
 @Autowired MockMvc mvc; @Autowired ObjectMapper om;
 @Test void upsertThenGetUser() throws Exception {
  String b = om.writeValueAsString(Map.of("userId","user-99","email","u99@example.com","status","ACTIVE"));
  mvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON).content(b)).andExpect(status().isOk());
  mvc.perform(get("/api/v1/users/user-99")).andExpect(status().isOk());
 }
}
