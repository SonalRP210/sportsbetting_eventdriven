package com.sportsbetting.platform.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityErrorBodyTest {

    @Test
    void unauthorized_and_forbidden_have_expected_error_text() {
        assertThat(SecurityErrorBody.unauthorized().error()).isEqualTo("Unauthorized");
        assertThat(SecurityErrorBody.forbidden().error()).isEqualTo("Forbidden");
    }
}
