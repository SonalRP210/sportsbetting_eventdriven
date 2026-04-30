package com.sportsbetting.riskservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportsbetting.riskservice.service.RiskService;
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
class RiskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RiskService riskService;

    @BeforeEach
    void resetState() {
        riskService.resetForTests();
    }

    @Test
    void betPlacedIncreasesUserAndTotalExposure() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of(
                "betId", "BET-1",
                "userId", "user-1",
                "openRisk", 120.50
        ));

        mockMvc.perform(post("/api/v1/internal/events/bet-placed")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isAccepted());

        mockMvc.perform(get("/api/v1/risk/users/user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openRisk", is(120.50)))
                .andExpect(jsonPath("$.openBetCount", is(1)));

        mockMvc.perform(get("/api/v1/risk/total"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExposure", is(120.50)));
    }

    @Test
    void betCancelledDecreasesExposure() throws Exception {
        String placed = objectMapper.writeValueAsString(Map.of(
                "betId", "BET-2",
                "userId", "user-2",
                "openRisk", 80.00
        ));
        String cancelled = objectMapper.writeValueAsString(Map.of(
                "betId", "BET-2",
                "userId", "user-2",
                "openRisk", 30.00
        ));

        mockMvc.perform(post("/api/v1/internal/events/bet-placed")
                .contentType(MediaType.APPLICATION_JSON).content(placed)).andExpect(status().isAccepted());
        mockMvc.perform(post("/api/v1/internal/events/bet-cancelled")
                .contentType(MediaType.APPLICATION_JSON).content(cancelled)).andExpect(status().isAccepted());

        mockMvc.perform(get("/api/v1/risk/users/user-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openRisk", is(50.00)))
                .andExpect(jsonPath("$.openBetCount", is(0)));
    }

    @Test
    void eventSettledReleasesExposureForMultipleUsers() throws Exception {
        mockMvc.perform(post("/api/v1/internal/events/bet-placed")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("betId", "B1", "userId", "u1", "openRisk", 70.0))))
                .andExpect(status().isAccepted());
        mockMvc.perform(post("/api/v1/internal/events/bet-placed")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("betId", "B2", "userId", "u2", "openRisk", 30.0))))
                .andExpect(status().isAccepted());

        String settled = objectMapper.writeValueAsString(Map.of(
                "eventId", "event-001",
                "winningSelection", "HOME",
                "releases", List.of(
                        Map.of("userId", "u1", "openRisk", 70.0),
                        Map.of("userId", "u2", "openRisk", 30.0)
                )
        ));

        mockMvc.perform(post("/api/v1/internal/events/event-settled")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(settled))
                .andExpect(status().isAccepted());

        mockMvc.perform(get("/api/v1/risk/total"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExposure", is(0.00)));

        mockMvc.perform(get("/api/v1/internal/outbox"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events.length()", is(4)));
    }
}
