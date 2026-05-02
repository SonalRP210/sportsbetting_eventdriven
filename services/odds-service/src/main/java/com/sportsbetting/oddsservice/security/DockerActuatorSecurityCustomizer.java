package com.sportsbetting.oddsservice.security;

import com.sportsbetting.platform.security.HttpApiAuthorizationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;

/**
 * Prometheus in the local Docker stack scrapes without a bearer token — permit selected actuator
 * endpoints only under the {@code docker} profile (not for production).
 */
@Configuration
@Profile("docker")
public class DockerActuatorSecurityCustomizer {

    @Bean
    @Order(10)
    HttpApiAuthorizationCustomizer dockerPermitScrapeActuatorEndpoints() {
        return auth -> auth.requestMatchers(
                        "/actuator/prometheus",
                        "/actuator/metrics",
                        "/actuator/metrics/**",
                        "/actuator/info"
                )
                .permitAll();
    }
}
