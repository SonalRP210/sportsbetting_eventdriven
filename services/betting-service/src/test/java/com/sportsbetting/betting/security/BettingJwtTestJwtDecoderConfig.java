package com.sportsbetting.betting.security;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Test-only JwtDecoder producing Keycloak-shaped claims without contacting a broker.
 */
@TestConfiguration
public class BettingJwtTestJwtDecoderConfig {

    private static final String ISS = "http://localhost/realms/test";

    @Bean
    @Primary
    JwtDecoder testJwtDecoder() {
        return token -> {
            List<String> roles;
            if (token.contains("bettor")) {
                roles = List.of("BET_READ", "BET_PLACE");
            } else if (token.contains("reader")) {
                roles = List.of("BET_READ");
            } else if (token.contains("ops")) {
                roles = List.of("BETTING_OPERATIONS", "BET_READ", "BET_PLACE");
            } else if (token.contains("empty")) {
                roles = List.of();
            } else {
                roles = List.of();
            }
            Instant now = Instant.now();
            return Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .issuedAt(now)
                    .expiresAt(now.plusSeconds(300))
                    .issuer(ISS)
                    .subject("test-user")
                    .claim("realm_access", Map.of("roles", roles))
                    .build();
        };
    }
}
