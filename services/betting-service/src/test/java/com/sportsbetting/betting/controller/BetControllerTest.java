package com.sportsbetting.betting.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportsbetting.betting.service.BettingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private BettingService bettingService;

    @BeforeEach
    void resetState() {
        bettingService.resetForTests();
    }

    @Test
    void pingEndpointReturnsOk() throws Exception {
        mockMvc.perform(get("/api/v1/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ok")));
    }
    @Test
    void placeBetAndReplayIdempotencyReturnsSameBetId() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of(
                "userId", "user-a",
                "eventId", "event-001",
                "selection", "HOME",
                "stake", 100
        ));

        String firstResponse = mockMvc.perform(post("/api/v1/bets")
                        .header("Idempotency-Key", "idem-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.betId", notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String firstBetId = objectMapper.readTree(firstResponse).get("betId").asText();

        mockMvc.perform(post("/api/v1/bets")
                        .header("Idempotency-Key", "idem-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.betId", is(firstBetId)));
    }

    @Test
    void getBetByIdReturnsBetDetails() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of(
                "userId", "user-b",
                "eventId", "event-001",
                "selection", "HOME",
                "stake", 55
        ));

        String response = mockMvc.perform(post("/api/v1/bets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String betId = objectMapper.readTree(response).get("betId").asText();

        mockMvc.perform(get("/api/v1/bets/{betId}", betId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.betId", is(betId)))
                .andExpect(jsonPath("$.status", is("OPEN")));
    }

    @Test
    void cancelBetChangesStatusAndWritesOutboxEvent() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of(
                "userId", "user-c",
                "eventId", "event-001",
                "selection", "AWAY",
                "stake", 25
        ));

        String response = mockMvc.perform(post("/api/v1/bets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String betId = objectMapper.readTree(response).get("betId").asText();

        mockMvc.perform(post("/api/v1/bets/{betId}/cancel", betId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CANCELLED")));

        mockMvc.perform(get("/api/v1/internal/outbox"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events.length()", is(2)))
                .andExpect(jsonPath("$.events[1].type", is("betting.bet.cancelled.v1")));
    }

    @Test
    void userBetsAndEventBetsReturnSummaries() throws Exception {
        mockMvc.perform(post("/api/v1/internal/odds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "eventId", "event-xyz",
                                "selection", "DRAW",
                                "odds", 3.0
                        ))))
                .andExpect(status().isAccepted());

        String payload = objectMapper.writeValueAsString(Map.of(
                "userId", "user-d",
                "eventId", "event-xyz",
                "selection", "DRAW",
                "stake", 10
        ));
        mockMvc.perform(post("/api/v1/bets").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/users/user-d/bets").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").doesNotExist())
                .andExpect(jsonPath("$[0].betId").exists());

        mockMvc.perform(get("/api/v1/events/event-xyz/bets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventId", is("event-xyz")));
    }

    @Test
    void setOddsValidatesPayload() throws Exception {
        mockMvc.perform(post("/api/v1/internal/odds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/internal/odds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "eventId", "e1",
                                "selection", "HOME",
                                "odds", 1.9
                        ))))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.accepted", is(true)));
    }

    @Test
    void dispatchOutboxReturnsCount() throws Exception {
        mockMvc.perform(post("/api/v1/internal/outbox/dispatch"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.published").exists());
    }
}


