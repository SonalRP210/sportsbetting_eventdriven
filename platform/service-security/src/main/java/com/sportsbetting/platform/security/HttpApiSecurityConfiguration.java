package com.sportsbetting.platform.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

import java.util.ArrayList;
import java.util.List;

@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class HttpApiSecurityConfiguration {

    @Bean
    SecurityFilterChain httpApiSecurityFilterChain(
            HttpSecurity http,
            HttpApiSecurityProperties props,
            ApiKeyAuthenticationFilter apiKeyAuthenticationFilter,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler,
            ObjectProvider<JwtAuthenticationConverter> jwtAuthenticationConverterProvider,
            List<HttpApiAuthorizationCustomizer> authorizationCustomizers
    ) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable);
        http.cors(AbstractHttpConfigurer::disable);
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.headers(headers -> headers
                .frameOptions(frame -> frame.deny())
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                        "default-src 'none'; frame-ancestors 'none'; base-uri 'none'"))
                .referrerPolicy(ref -> ref.policy(
                        ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)));

        http.exceptionHandling(ex -> ex
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler));

        if (!props.isEnabled()) {
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }

        List<HttpApiAuthorizationCustomizer> orderedCustomizers =
                authorizationCustomizers == null ? List.of() : new ArrayList<>(authorizationCustomizers);

        http.authorizeHttpRequests(auth -> {
            auth.requestMatchers("/actuator/health", "/actuator/health/**").permitAll();
            for (HttpApiAuthorizationCustomizer customizer : orderedCustomizers) {
                customizer.customize(auth);
            }
            auth.requestMatchers("/api/**", "/actuator/**").authenticated();
            auth.anyRequest().denyAll();
        });

        if (props.getAuthType() == AuthType.API_KEY) {
            http.addFilterBefore(apiKeyAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        } else {
            JwtAuthenticationConverter converter = jwtAuthenticationConverterProvider.getIfAvailable(
                    JwtAuthenticationConverter::new);
            http.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(converter)));
        }

        return http.build();
    }

    @Bean
    ApiKeyAuthenticationFilter apiKeyAuthenticationFilter(HttpApiSecurityProperties props) {
        return new ApiKeyAuthenticationFilter(props);
    }

    @Bean
    RestAuthenticationEntryPoint restAuthenticationEntryPoint(ObjectMapper objectMapper) {
        return new RestAuthenticationEntryPoint(objectMapper);
    }

    @Bean
    RestAccessDeniedHandler restAccessDeniedHandler(ObjectMapper objectMapper) {
        return new RestAccessDeniedHandler(objectMapper);
    }
}
