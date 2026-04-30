package com.sportsbetting.settlementservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportsbetting.settlementservice.service.SettlementService;
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
class SettlementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SettlementService settlementService;

    @BeforeEach
    void resetState() {
        settlementService.resetForTests();
        String bet1 = "{\"betId\":\"BET-1\",\"userId\":\"user-1\",\"eventId\":\"event-001\",\"selection\":\"HOME\",\"stake\":100.00,\"odds\":1.95,\"status\":\"OPEN\"}";
        String bet2 = "{\"betId\":\"BET-2\",\"userId\":\"user-2\",\"eventId\":\"event-001\",\"selection\":\"AWAY\",\"stake\":80.00,\"odds\":2.10,\"status\":\"OPEN\"}";
        try {
            mockMvc.perform(post("/api/v1/internal/open-bets").contentType(MediaType.APPLICATION_JSON).content(bet1)).andExpect(status().isAccepted());
            mockMvc.perform(post("/api/v1/internal/open-bets").contentType(MediaType.APPLICATION_JSON).content(bet2)).andExpect(status().isAccepted());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test
    void settleEventMarksWinnersAndLosersAndPublishesEvent() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of(
                "eventId", "event-001",
                "winningSelection", "HOME"
        ));

        mockMvc.perform(post("/api/v1/events/settlements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.winners", is(1)))
                .andExpect(jsonPath("$.losers", is(1)))
                .andExpect(jsonPath("$.totalPayout", is(195.00)));

        mockMvc.perform(get("/api/v1/internal/positions/BET-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("WON")));

        mockMvc.perform(get("/api/v1/internal/positions/BET-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("LOST")));

        mockMvc.perform(get("/api/v1/internal/outbox"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events.length()", is(1)))
                .andExpect(jsonPath("$.events[0].type", is("settlement.event.settled.v1")));
    }

    @Test
    void repeatedSettlementWithSameWinnerIsIdempotent() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of(
                "eventId", "event-001",
                "winningSelection", "HOME"
        ));

        mockMvc.perform(post("/api/v1/events/settlements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/events/settlements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.winners", is(1)));
    }

    @Test
    void repeatedSettlementWithDifferentWinnerReturnsConflict() throws Exception {
        String first = objectMapper.writeValueAsString(Map.of(
                "eventId", "event-001",
                "winningSelection", "HOME"
        ));
        String second = objectMapper.writeValueAsString(Map.of(
                "eventId", "event-001",
                "winningSelection", "AWAY"
        ));

        mockMvc.perform(post("/api/v1/events/settlements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(first))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/events/settlements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(second))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("SETTLEMENT_CONFLICT")));
    }
}
