package com.sportsbetting.eventingestionservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportsbetting.eventingestionservice.service.IngestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class IngestionControllerTest {
 @Autowired MockMvc mvc; @Autowired ObjectMapper om; @Autowired IngestionService service;
 @BeforeEach void reset(){ service.resetForTests(); }
 @Test void ingestsProviderEvent() throws Exception {
  String b = om.writeValueAsString(Map.of("provider","sportradar","eventType","match-started","payload",Map.of("matchId","m1")));
  mvc.perform(post("/api/v1/providers/events").contentType(MediaType.APPLICATION_JSON).content(b)).andExpect(status().isAccepted());
  mvc.perform(get("/api/v1/internal/normalized-events")).andExpect(status().isOk()).andExpect(jsonPath("$.events.length()", is(1)));
 }
}
