package com.sportsbetting.platform.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public class HttpApiSecurityProperties {

    /**
     * When true, {@code /api/**} and non-health actuator endpoints require authentication.
     */
    private boolean enabled = false;

    private AuthType authType = AuthType.JWT;

    private String apiKeyHeader = "X-API-Key";

    /**
     * Shared secret for {@link AuthType#API_KEY}; bind via {@code APP_SECURITY_API_KEY}.
     */
    private String apiKey = "";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public AuthType getAuthType() {
        return authType;
    }

    public void setAuthType(AuthType authType) {
        this.authType = authType;
    }

    public String getApiKeyHeader() {
        return apiKeyHeader;
    }

    public void setApiKeyHeader(String apiKeyHeader) {
        this.apiKeyHeader = apiKeyHeader;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
