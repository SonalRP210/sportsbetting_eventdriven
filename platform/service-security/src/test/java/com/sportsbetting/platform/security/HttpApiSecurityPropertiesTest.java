package com.sportsbetting.platform.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HttpApiSecurityPropertiesTest {

    @Test
    void defaults_match_expected_bootstrap_contract() {
        HttpApiSecurityProperties p = new HttpApiSecurityProperties();
        assertThat(p.isEnabled()).isFalse();
        assertThat(p.getAuthType()).isEqualTo(AuthType.JWT);
        assertThat(p.getApiKeyHeader()).isEqualTo("X-API-Key");
        assertThat(p.getApiKey()).isEmpty();
    }

    @Test
    void setters_roundTrip() {
        HttpApiSecurityProperties p = new HttpApiSecurityProperties();
        p.setEnabled(true);
        p.setAuthType(AuthType.API_KEY);
        p.setApiKeyHeader("X-Custom");
        p.setApiKey("secret");
        assertThat(p.isEnabled()).isTrue();
        assertThat(p.getAuthType()).isEqualTo(AuthType.API_KEY);
        assertThat(p.getApiKeyHeader()).isEqualTo("X-Custom");
        assertThat(p.getApiKey()).isEqualTo("secret");
    }
}
