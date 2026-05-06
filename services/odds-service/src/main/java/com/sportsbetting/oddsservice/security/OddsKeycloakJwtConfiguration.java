package com.sportsbetting.oddsservice.security;

import com.sportsbetting.platform.security.HttpApiAuthorizationCustomizer;
import com.sportsbetting.platform.security.KeycloakJwtSupport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

@Configuration
@ConditionalOnProperty(name = "app.security.enabled", havingValue = "true")
public class OddsKeycloakJwtConfiguration {

    @Bean
    @ConditionalOnProperty(name = "app.security.auth-type", havingValue = "jwt")
    JwtAuthenticationConverter oddsResourceServerJwtAuthenticationConverter() {
        return KeycloakJwtSupport.realmRolesJwtAuthenticationConverter();
    }

    @Bean
    @Order(50)
    @ConditionalOnProperty(name = "app.security.auth-type", havingValue = "jwt")
    HttpApiAuthorizationCustomizer oddsRoleBasedApiAuthorization() {
        return auth -> {
            auth.requestMatchers(HttpMethod.POST, "/api/v1/odds-feed")
                    .hasAnyRole("ODDS_FEED_WRITE", "ODDS_ADMIN");
            auth.requestMatchers(HttpMethod.GET, "/api/v1/odds/**")
                    .hasAnyRole("ODDS_READ", "ODDS_FEED_WRITE", "ODDS_ADMIN");
            auth.requestMatchers(HttpMethod.GET, "/api/v1/internal/outbox")
                    .hasAnyRole("ODDS_OPERATIONS", "ODDS_ADMIN");
            auth.requestMatchers(HttpMethod.POST, "/api/v1/internal/outbox/dispatch")
                    .hasAnyRole("ODDS_OPERATIONS", "ODDS_ADMIN");
            auth.requestMatchers(HttpMethod.POST, "/api/v1/internal/seed-odds")
                    .hasRole("ODDS_ADMIN");
        };
    }
}
