package com.sportsbetting.betting.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportsbetting.betting.service.BettingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(BettingJwtTestJwtDecoderConfig.class)
@TestPropertySource(
        properties = {
                "app.security.enabled=true",
                "app.security.auth-type=jwt",
                "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost/realms/test",
        }
)
class BettingJwtAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BettingService bettingService;

    @BeforeEach
    void reset() {
        bettingService.resetForTests();
    }

    @Test
    void pingPermittedWithoutJwt() throws Exception {
        mockMvc.perform(get("/api/v1/ping")).andExpect(status().isOk());
    }

    @Test
    void placeBetUnauthorizedWithoutJwt() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of(
                "userId", "user-1",
                "eventId", "event-001",
                "selection", "HOME",
                "stake", 10
        ));
        mockMvc.perform(post("/api/v1/bets").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void placeBetForbiddenWhenReaderJwt() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of(
                "userId", "user-1",
                "eventId", "event-001",
                "selection", "HOME",
                "stake", 10
        ));
        mockMvc.perform(post("/api/v1/bets")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-reader-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden());
    }

    @Test
    void placeBetCreatedWhenBettorJwt() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of(
                "userId", "user-1",
                "eventId", "event-001",
                "selection", "HOME",
                "stake", 10
        ));
        mockMvc.perform(post("/api/v1/bets")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-bettor-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.betId").exists());
    }

    @Test
    void readBetForbiddenWhenJwtHasNoRealmRoles() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of(
                "userId", "user-1",
                "eventId", "event-001",
                "selection", "HOME",
                "stake", 5
        ));
        String response = mockMvc.perform(post("/api/v1/bets")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-bettor-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String betId = objectMapper.readTree(response).get("betId").asText();

        mockMvc.perform(get("/api/v1/bets/" + betId).header(HttpHeaders.AUTHORIZATION, "Bearer test-empty-1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void internalOutboxForbiddenForBettor() throws Exception {
        mockMvc.perform(get("/api/v1/internal/outbox").header(HttpHeaders.AUTHORIZATION, "Bearer test-bettor-1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void internalOutboxOkForOpsJwt() throws Exception {
        mockMvc.perform(get("/api/v1/internal/outbox").header(HttpHeaders.AUTHORIZATION, "Bearer test-ops-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events").isArray());
    }
}
