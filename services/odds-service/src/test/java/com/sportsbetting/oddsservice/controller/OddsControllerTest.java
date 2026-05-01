package com.sportsbetting.oddsservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportsbetting.oddsservice.service.OddsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OddsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OddsService oddsService;

    @BeforeEach
    void resetState() {
        oddsService.resetForTests();
    }

    @Test
    void oddsFeedStoresOddsAndPublishesOutboxEvent() throws Exception {
        String payload = objectMapper.writeValueAsString(List.of(Map.of(
                "eventId", "evt-1",
                "selection", "HOME",
                "odds", 1.75
        )));

        mockMvc.perform(post("/api/v1/odds-feed")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isAccepted());

        mockMvc.perform(get("/api/v1/odds/evt-1/HOME"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.odds", is(1.75)));

        mockMvc.perform(get("/api/v1/internal/outbox"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events.length()", is(1)))
                .andExpect(jsonPath("$.events[0].type", is("odds.updated.v1")));
    }

    @Test
    void getOddsReturns404WhenMissing() throws Exception {
        mockMvc.perform(get("/api/v1/odds/missing/HOME"))
                .andExpect(status().isNotFound());
    }

    @Test
    void oddsFeedRejectsInvalidOdds() throws Exception {
        String payload = objectMapper.writeValueAsString(List.of(Map.of(
                "eventId", "evt-1",
                "selection", "HOME",
                "odds", 0
        )));

        mockMvc.perform(post("/api/v1/odds-feed")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }
}
