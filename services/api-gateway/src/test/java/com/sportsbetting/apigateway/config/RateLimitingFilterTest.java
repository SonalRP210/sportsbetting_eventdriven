package com.sportsbetting.apigateway.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitingFilterTest {

    @Test
    void returns429AfterBurstOnSameKey() throws ServletException, IOException {
        RateLimitingFilter filter = new RateLimitingFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.setRequestURI("/api/v1/gateway/health");
        FilterChain chain = (req, res) -> {};

        for (int i = 0; i < 300; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilterInternal(request, response, chain);
            assertThat(response.getStatus()).isEqualTo(200);
        }
        MockHttpServletResponse blocked = new MockHttpServletResponse();
        filter.doFilterInternal(request, blocked, chain);
        assertThat(blocked.getStatus()).isEqualTo(429);
    }

    @Test
    void differentUrisAreBucketedSeparately() throws ServletException, IOException {
        RateLimitingFilter filter = new RateLimitingFilter();
        FilterChain chain = (req, res) -> {};

        MockHttpServletRequest a = new MockHttpServletRequest();
        a.setRemoteAddr("10.0.0.2");
        a.setRequestURI("/path-a");
        MockHttpServletResponse ra = new MockHttpServletResponse();
        filter.doFilterInternal(a, ra, chain);
        assertThat(ra.getStatus()).isEqualTo(200);

        MockHttpServletRequest b = new MockHttpServletRequest();
        b.setRemoteAddr("10.0.0.2");
        b.setRequestURI("/path-b");
        MockHttpServletResponse rb = new MockHttpServletResponse();
        filter.doFilterInternal(b, rb, chain);
        assertThat(rb.getStatus()).isEqualTo(200);
    }
}
