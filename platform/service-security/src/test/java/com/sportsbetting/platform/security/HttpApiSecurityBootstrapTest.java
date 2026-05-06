package com.sportsbetting.platform.security;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

class HttpApiSecurityBootstrapTest {

    @Test
    void when_disabled_does_not_validate_credentials() {
        HttpApiSecurityProperties props = new HttpApiSecurityProperties();
        props.setEnabled(false);
        Environment env = Mockito.mock(Environment.class);
        new HttpApiSecurityBootstrap(props, env);
    }

    @Test
    void when_enabled_api_key_requires_non_blank_secret() {
        HttpApiSecurityProperties props = new HttpApiSecurityProperties();
        props.setEnabled(true);
        props.setAuthType(AuthType.API_KEY);
        props.setApiKey(" ");
        Environment env = Mockito.mock(Environment.class);
        assertThatThrownBy(() -> new HttpApiSecurityBootstrap(props, env))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("api-key");
    }

    @Test
    void when_enabled_api_key_with_secret_ok() {
        HttpApiSecurityProperties props = new HttpApiSecurityProperties();
        props.setEnabled(true);
        props.setAuthType(AuthType.API_KEY);
        props.setApiKey("valid");
        Environment env = Mockito.mock(Environment.class);
        new HttpApiSecurityBootstrap(props, env);
    }

    @Test
    void when_enabled_jwt_requires_issuer_uri() {
        HttpApiSecurityProperties props = new HttpApiSecurityProperties();
        props.setEnabled(true);
        props.setAuthType(AuthType.JWT);
        Environment env = Mockito.mock(Environment.class);
        when(env.getProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri")).thenReturn(null);
        assertThatThrownBy(() -> new HttpApiSecurityBootstrap(props, env))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("issuer-uri");
    }

    @Test
    void when_enabled_jwt_with_issuer_ok() {
        HttpApiSecurityProperties props = new HttpApiSecurityProperties();
        props.setEnabled(true);
        props.setAuthType(AuthType.JWT);
        Environment env = Mockito.mock(Environment.class);
        when(env.getProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri"))
                .thenReturn("https://issuer.example/");
        new HttpApiSecurityBootstrap(props, env);
    }
}
