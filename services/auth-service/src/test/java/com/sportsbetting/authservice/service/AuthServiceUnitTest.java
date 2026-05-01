package com.sportsbetting.authservice.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthServiceUnitTest {

    private final AuthService authService = new AuthService();

    @Test
    void loginReturnsTokenEnvelope() {
        assertThat(authService.login("user-1", "secret"))
                .containsEntry("tokenType", "Bearer")
                .containsKey("accessToken");
    }

    @Test
    void loginRejectsInvalidCredentials() {
        assertThatThrownBy(() -> authService.login("user-1", " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("INVALID_CREDENTIALS");
    }
}
