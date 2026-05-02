package com.sportsbetting.platform.security;

import com.sportsbetting.platform.security.support.SecurityIntegrationTestApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = SecurityIntegrationTestApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(
        properties = {
            "app.security.enabled=true",
            "app.security.auth-type=api-key",
            "app.security.api-key=integration-secret",
            "management.endpoints.web.exposure.include=health",
        }
)
class HttpApiSecurityApiKeyIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void actuator_health_permitted_without_api_key() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void api_requires_api_key() throws Exception {
        mockMvc.perform(get("/api/ping")).andExpect(status().isUnauthorized()).andExpect(content().json("{\"error\":\"Unauthorized\"}"));
    }

    @Test
    void api_accepts_valid_api_key() throws Exception {
        mockMvc.perform(get("/api/ping").header("X-API-Key", "integration-secret"))
                .andExpect(status().isOk())
                .andExpect(content().string("pong"));
    }

    @Test
    void non_api_paths_return_unauthorized_when_anonymous() throws Exception {
        mockMvc.perform(get("/other"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().json("{\"error\":\"Unauthorized\"}"));
    }
}
