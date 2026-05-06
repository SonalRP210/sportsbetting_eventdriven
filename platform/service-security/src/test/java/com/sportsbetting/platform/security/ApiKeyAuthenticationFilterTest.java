package com.sportsbetting.platform.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ApiKeyAuthenticationFilterTest {

    @Test
    void constantTimeEquals_requiresExactMatch() {
        assertThat(ApiKeyAuthenticationFilter.constantTimeEquals("secret", "secret")).isTrue();
        assertThat(ApiKeyAuthenticationFilter.constantTimeEquals("secret", "wrong")).isFalse();
        assertThat(ApiKeyAuthenticationFilter.constantTimeEquals("secret", null)).isFalse();
        assertThat(ApiKeyAuthenticationFilter.constantTimeEquals(null, "x")).isFalse();
    }

    @Test
    void shouldNotFilter_when_disabled() {
        HttpApiSecurityProperties props = new HttpApiSecurityProperties();
        props.setEnabled(false);
        ApiKeyAuthenticationFilter filter = new ApiKeyAuthenticationFilter(props);
        assertThat(filter.shouldNotFilter(new MockHttpServletRequest())).isTrue();
    }

    @Test
    void shouldNotFilter_when_auth_type_not_api_key() {
        HttpApiSecurityProperties props = new HttpApiSecurityProperties();
        props.setEnabled(true);
        props.setAuthType(AuthType.JWT);
        ApiKeyAuthenticationFilter filter = new ApiKeyAuthenticationFilter(props);
        assertThat(filter.shouldNotFilter(new MockHttpServletRequest())).isTrue();
    }

    @Test
    void shouldNotFilter_for_actuator_health() {
        HttpApiSecurityProperties props = new HttpApiSecurityProperties();
        props.setEnabled(true);
        props.setAuthType(AuthType.API_KEY);
        ApiKeyAuthenticationFilter filter = new ApiKeyAuthenticationFilter(props);
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/actuator/health");
        assertThat(filter.shouldNotFilter(req)).isTrue();
    }

    @Test
    void wrong_key_does_not_set_authentication() throws Exception {
        SecurityContextHolder.clearContext();
        HttpApiSecurityProperties props = new HttpApiSecurityProperties();
        props.setEnabled(true);
        props.setAuthType(AuthType.API_KEY);
        props.setApiKey("expected");
        props.setApiKeyHeader("X-API-Key");
        ApiKeyAuthenticationFilter filter = new ApiKeyAuthenticationFilter(props);
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-API-Key", "wrong");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(req, res, chain);
        verify(chain).doFilter(req, res);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void matching_key_sets_service_client_role() throws Exception {
        SecurityContextHolder.clearContext();
        HttpApiSecurityProperties props = new HttpApiSecurityProperties();
        props.setEnabled(true);
        props.setAuthType(AuthType.API_KEY);
        props.setApiKey("expected");
        props.setApiKeyHeader("X-API-Key");
        ApiKeyAuthenticationFilter filter = new ApiKeyAuthenticationFilter(props);
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-API-Key", "expected");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(req, res, chain);
        verify(chain).doFilter(any(), any());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .anyMatch(a -> a.getAuthority().equals(ApiKeyAuthenticationFilter.ROLE_HTTP_SERVICE_CLIENT));
    }
}
