package com.sportsbetting.betting.security;

import com.sportsbetting.platform.security.HttpApiAuthorizationCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;

/**
 * Paths that stay public regardless of {@code app.security.auth-type} (JWT customizers are not
 * registered for API-key mode, so permit rules that must apply to both modes live here).
 */
@Configuration
@ConditionalOnProperty(name = "app.security.enabled", havingValue = "true")
public class BettingPublicPathsSecurityConfiguration {

    @Bean
    @Order(5)
    HttpApiAuthorizationCustomizer bettingPublicPaths() {
        return auth -> auth.requestMatchers(HttpMethod.GET, "/api/v1/ping").permitAll();
    }
}
