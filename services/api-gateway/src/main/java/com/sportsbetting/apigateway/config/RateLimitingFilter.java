package com.sportsbetting.apigateway.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {
    private static final int LIMIT_PER_MINUTE = 300;
    private final Map<String, CounterBucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String key = request.getRemoteAddr() + ":" + request.getRequestURI();
        long minute = Instant.now().getEpochSecond() / 60;
        CounterBucket bucket = buckets.compute(key, (k, b) -> (b == null || b.minute != minute) ? new CounterBucket(minute, 1) : new CounterBucket(minute, b.count + 1));
        if (bucket.count > LIMIT_PER_MINUTE) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"code\":\"RATE_LIMITED\",\"message\":\"Too many requests. Please retry later.\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private record CounterBucket(long minute, int count) {}
}
