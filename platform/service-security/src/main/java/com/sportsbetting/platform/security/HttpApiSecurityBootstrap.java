package com.sportsbetting.platform.security;

import org.springframework.core.env.Environment;

public final class HttpApiSecurityBootstrap {

    public HttpApiSecurityBootstrap(HttpApiSecurityProperties props, Environment env) {
        if (!props.isEnabled()) {
            return;
        }
        switch (props.getAuthType()) {
            case API_KEY -> {
                if (props.getApiKey() == null || props.getApiKey().isBlank()) {
                    throw new IllegalStateException(
                            "app.security.api-key (or APP_SECURITY_API_KEY) is required when "
                                    + "app.security.enabled=true and auth-type=api-key");
                }
            }
            case JWT -> {
                String issuer = env.getProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri");
                if (issuer == null || issuer.isBlank()) {
                    throw new IllegalStateException(
                            "spring.security.oauth2.resourceserver.jwt.issuer-uri (or JWT_ISSUER_URI) is required "
                                    + "when app.security.enabled=true and auth-type=jwt");
                }
            }
        }
    }
}
