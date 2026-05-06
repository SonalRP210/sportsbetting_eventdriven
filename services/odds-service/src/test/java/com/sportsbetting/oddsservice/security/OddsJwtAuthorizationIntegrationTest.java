package com.sportsbetting.oddsservice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import com.sportsbetting.oddsservice.model.odds.OddsUpdate;
import com.sportsbetting.oddsservice.repository.OddsQuoteRepository;
import com.sportsbetting.oddsservice.repository.OutboxEventRepository;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(OddsJwtTestJwtDecoderConfig.class)
@TestPropertySource(
        properties = {
                "app.security.enabled=true",
                "app.security.auth-type=jwt",
                "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost/realms/test",
        }
)
class OddsJwtAuthorizationIntegrationTest {

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
    void oddsFeedForbiddenWithoutJwt() throws Exception {
        String payload = objectMapper.writeValueAsString(List.of(
                new OddsUpdate("jwt-e", "HOME", new BigDecimal("3.5"))
        ));
        mockMvc.perform(post("/api/v1/odds-feed").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void oddsFeedForbiddenWhenReaderJwt() throws Exception {
        String payload = objectMapper.writeValueAsString(List.of(
                new OddsUpdate("jwt-e", "HOME", new BigDecimal("3.5"))
        ));
        mockMvc.perform(post("/api/v1/odds-feed")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-reader-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden());
    }

    @Test
    void oddsFeedAcceptedWhenFeederJwt() throws Exception {
        String payload = objectMapper.writeValueAsString(List.of(
                new OddsUpdate("jwt-e", "HOME", new BigDecimal("3.5"))
        ));
        mockMvc.perform(post("/api/v1/odds-feed")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-feeder-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isAccepted());
    }

    @Test
    void readOddsForbiddenWhenJwtHasNoRealmRoles() throws Exception {
        mockMvc.perform(get("/api/v1/odds/no/here").header(HttpHeaders.AUTHORIZATION, "Bearer test-empty-1"))
                .andExpect(status().isForbidden());
    }
}
