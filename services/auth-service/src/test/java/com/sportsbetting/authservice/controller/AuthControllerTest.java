package com.sportsbetting.authservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {
 @Autowired MockMvc mvc; @Autowired ObjectMapper om;
 @Test void loginSuccess() throws Exception {
  String b = om.writeValueAsString(Map.of("username","user1","password","secret"));
  mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(b)).andExpect(status().isOk());
 }
}
