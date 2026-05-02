package com.sportsbetting.betting.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportsbetting.betting.service.BettingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
@TestPropertySource(
        properties = {
                "app.security.enabled=true",
                "app.security.auth-type=api-key",
                "app.security.api-key=integration-test-secret",
        }
)
class BettingSecurityApiKeyIntegrationTest {

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
    void actuatorHealthPermittedWithoutCredentials() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void apiEndpointsRejectMissingApiKey() throws Exception {
        mockMvc.perform(get("/api/v1/users/user-1/bets")).andExpect(status().isUnauthorized());
    }

    @Test
    void apiEndpointsRejectWrongApiKey() throws Exception {
        mockMvc.perform(get("/api/v1/users/user-1/bets").header("X-API-Key", "wrong"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void pingPermittedWithoutApiKey() throws Exception {
        mockMvc.perform(get("/api/v1/ping")).andExpect(status().isOk());
    }

    @Test
    void prometheusEndpointRequiresAuthWhenSecurityEnabled() throws Exception {
        mockMvc.perform(get("/actuator/prometheus")).andExpect(status().isUnauthorized());
    }

    @Test
    void placeBetWorksWithApiKey() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of(
                "userId", "user-1",
                "eventId", "event-001",
                "selection", "HOME",
                "stake", 10
        ));

        mockMvc.perform(post("/api/v1/bets")
                        .header("X-API-Key", "integration-test-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.betId").exists());

        mockMvc.perform(get("/api/v1/users/user-1/bets").header("X-API-Key", "integration-test-secret"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].betId").exists());
    }
}
