package com.sportsbetting.betting.security;

import com.sportsbetting.platform.security.HttpApiAuthorizationCustomizer;
import com.sportsbetting.platform.security.KeycloakJwtSupport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

/**
 * JWT RBAC for betting HTTP APIs. When {@code app.security.auth-type=api-key}, these beans are
 * not registered; only {@code authenticated()} applies (same pattern as odds-service).
 */
@Configuration
@ConditionalOnProperty(name = "app.security.enabled", havingValue = "true")
public class BettingKeycloakJwtConfiguration {

    @Bean
    @ConditionalOnProperty(name = "app.security.auth-type", havingValue = "jwt")
    JwtAuthenticationConverter bettingResourceServerJwtAuthenticationConverter() {
        return KeycloakJwtSupport.realmRolesJwtAuthenticationConverter();
    }

    /**
     * Public: {@code GET /api/v1/ping} (load balancers / compose health).<br>
     * Customer APIs: place/list/cancel bets require {@code BET_READ} / {@code BET_PLACE}.<br>
     * Internal: odds seed, outbox inspection, and manual dispatch require {@code BETTING_OPERATIONS}.
     */
    @Bean
    @Order(50)
    @ConditionalOnProperty(name = "app.security.auth-type", havingValue = "jwt")
    HttpApiAuthorizationCustomizer bettingRoleBasedApiAuthorization() {
        return auth -> {
            auth.requestMatchers(HttpMethod.GET, "/api/v1/ping").permitAll();

            auth.requestMatchers(HttpMethod.POST, "/api/v1/bets")
                    .hasAnyRole("BET_PLACE", "BETTING_ADMIN");
            auth.requestMatchers(HttpMethod.GET, "/api/v1/bets/**")
                    .hasAnyRole("BET_READ", "BET_PLACE", "BETTING_ADMIN");
            auth.requestMatchers(HttpMethod.POST, "/api/v1/bets/*/cancel")
                    .hasAnyRole("BET_PLACE", "BETTING_ADMIN");

            auth.requestMatchers(HttpMethod.GET, "/api/v1/users/*/bets")
                    .hasAnyRole("BET_READ", "BET_PLACE", "BETTING_ADMIN");
            auth.requestMatchers(HttpMethod.GET, "/api/v1/events/*/bets")
                    .hasAnyRole("BET_READ", "BET_PLACE", "BETTING_ADMIN");

            auth.requestMatchers(HttpMethod.POST, "/api/v1/internal/odds")
                    .hasAnyRole("BETTING_OPERATIONS", "BETTING_ADMIN");
            auth.requestMatchers(HttpMethod.GET, "/api/v1/internal/outbox")
                    .hasAnyRole("BETTING_OPERATIONS", "BETTING_ADMIN");
            auth.requestMatchers(HttpMethod.POST, "/api/v1/internal/outbox/dispatch")
                    .hasAnyRole("BETTING_OPERATIONS", "BETTING_ADMIN");
        };
    }
}
