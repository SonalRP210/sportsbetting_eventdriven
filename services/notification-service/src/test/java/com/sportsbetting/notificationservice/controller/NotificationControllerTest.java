package com.sportsbetting.notificationservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportsbetting.notificationservice.service.NotificationService;
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
class NotificationControllerTest {
 @Autowired MockMvc mvc; @Autowired ObjectMapper om; @Autowired NotificationService service;
 @BeforeEach void reset(){ service.resetForTests(); }
 @Test void consumesAndStoresDelivery() throws Exception {
   String body = om.writeValueAsString(Map.of("eventType","betting.bet.placed.v1","userId","u1","data",Map.of("betId","B1")));
   mvc.perform(post("/api/v1/internal/events").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isAccepted());
   mvc.perform(get("/api/v1/internal/deliveries")).andExpect(status().isOk()).andExpect(jsonPath("$.deliveries.length()", is(1)));
 }
}
