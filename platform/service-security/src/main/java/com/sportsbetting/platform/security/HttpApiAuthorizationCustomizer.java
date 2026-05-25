package com.sportsbetting.platform.security;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

/**
 * Optional path-based rules (RBAC) applied after health is permitted and before the generic
 * {@code /api/**} + {@code /actuator/**} authenticated fallback. Register one or more beans;
 * use {@code @Order} if matching precedence matters.
 */
@FunctionalInterface
public interface HttpApiAuthorizationCustomizer {
    void customize(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth);
}
