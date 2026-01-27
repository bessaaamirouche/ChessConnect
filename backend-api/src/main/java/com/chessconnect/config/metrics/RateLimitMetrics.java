package com.chessconnect.config.metrics;

import com.chessconnect.security.RateLimitingFilter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class RateLimitMetrics {

    public RateLimitMetrics(MeterRegistry meterRegistry, RateLimitingFilter rateLimitingFilter) {
        // Register metrics for rate limiting
        Gauge.builder("rate_limit_blocked_requests_total", rateLimitingFilter, RateLimitingFilter::getTotalBlockedRequests)
                .description("Total number of requests blocked by rate limiting")
                .register(meterRegistry);

        Gauge.builder("rate_limit_active_entries", rateLimitingFilter, RateLimitingFilter::getActiveRateLimitEntries)
                .description("Number of active rate limit tracking entries")
                .register(meterRegistry);
    }
}
