package com.sportsbetting.oddsservice.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportsbetting.oddsservice.model.odds.OddsUpdate;
import com.sportsbetting.oddsservice.repository.OddsQuoteRepository;
import com.sportsbetting.oddsservice.repository.OutboxEventRepository;

import java.math.BigDecimal;
import java.util.List;

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
class OddsSecurityApiKeyIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    OddsQuoteRepository oddsQuoteRepository;

    @Autowired
    OutboxEventRepository outboxEventRepository;

    @BeforeEach
    void clean() {
        outboxEventRepository.deleteAll();
        oddsQuoteRepository.deleteAll();
    }

    @Test
    void actuatorHealthPermittedWithoutCredentials() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void apiEndpointsRejectMissingApiKey() throws Exception {
        mockMvc.perform(get("/api/v1/odds/e/s")).andExpect(status().isUnauthorized());
    }

    @Test
    void apiEndpointsRejectWrongApiKey() throws Exception {
        mockMvc.perform(get("/api/v1/odds/e/s").header("X-API-Key", "wrong")).andExpect(status().isUnauthorized());
    }

    @Test
    void apiAcceptsValidApiKey() throws Exception {
        mockMvc.perform(get("/api/v1/odds/e/s").header("X-API-Key", "integration-test-secret"))
                .andExpect(status().isNotFound());
    }

    @Test
    void prometheusEndpointRequiresAuthWhenSecurityEnabled() throws Exception {
        mockMvc.perform(get("/actuator/prometheus")).andExpect(status().isUnauthorized());
    }

    @Test
    void oddsFeedWorksWithApiKey() throws Exception {
        String payload = objectMapper.writeValueAsString(List.of(
                new OddsUpdate("evt-sec", "HOME", new BigDecimal("2.5"))
        ));

        mockMvc.perform(post("/api/v1/odds-feed")
                        .header("X-API-Key", "integration-test-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isAccepted());

        mockMvc.perform(get("/api/v1/odds/evt-sec/HOME").header("X-API-Key", "integration-test-secret"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.odds", is(2.5)));
    }
}
