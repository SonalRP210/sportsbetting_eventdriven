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
            "app.security.enabled=false",
        }
)
class HttpApiSecurityDisabledIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void api_route_permitted_without_credentials() throws Exception {
        mockMvc.perform(get("/api/ping")).andExpect(status().isOk()).andExpect(content().string("pong"));
    }
}
