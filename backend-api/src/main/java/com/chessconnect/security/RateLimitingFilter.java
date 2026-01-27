package com.chessconnect.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);

    // Rate limits per minute by endpoint category
    private static final int RATE_LIMIT_AUTH = 5;           // Login, register, password reset
    private static final int RATE_LIMIT_PAYMENT = 10;       // Payment operations
    private static final int RATE_LIMIT_BOOKING = 20;       // Lesson booking
    private static final int RATE_LIMIT_UPLOAD = 5;         // File uploads
    private static final int RATE_LIMIT_CONTACT = 3;        // Contact form
    private static final int RATE_LIMIT_API_WRITE = 30;     // POST/PUT/DELETE general
    private static final int RATE_LIMIT_API_READ = 100;     // GET requests
    private static final int RATE_LIMIT_GLOBAL = 200;       // Global per IP

    private static final long WINDOW_MS = 60_000; // 1 minute window

    private final Map<String, RateLimitEntry> requestCounts = new ConcurrentHashMap<>();
    private final AtomicLong totalBlockedRequests = new AtomicLong(0);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();
        String clientIp = getClientIp(request);

        // Skip rate limiting for health checks and static resources
        if (isExcludedPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Determine rate limit based on endpoint
        int maxRequests = getRateLimit(path, method);

        // Create a key based on IP and endpoint category
        String endpointCategory = getEndpointCategory(path);
        String key = clientIp + ":" + endpointCategory;

        // Also check global rate limit per IP
        String globalKey = clientIp + ":global";

        // Check endpoint-specific limit
        if (!checkRateLimit(key, maxRequests)) {
            handleRateLimitExceeded(response, clientIp, path, "endpoint");
            return;
        }

        // Check global limit
        if (!checkRateLimit(globalKey, RATE_LIMIT_GLOBAL)) {
            handleRateLimitExceeded(response, clientIp, path, "global");
            return;
        }

        // Add rate limit headers
        RateLimitEntry entry = requestCounts.get(key);
        if (entry != null) {
            int remaining = Math.max(0, maxRequests - entry.count.get());
            response.setHeader("X-RateLimit-Limit", String.valueOf(maxRequests));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
            response.setHeader("X-RateLimit-Reset", String.valueOf((entry.windowStart + WINDOW_MS) / 1000));
        }

        filterChain.doFilter(request, response);
    }

    private boolean checkRateLimit(String key, int maxRequests) {
        RateLimitEntry entry = requestCounts.compute(key, (k, v) -> {
            long now = System.currentTimeMillis();
            if (v == null || now - v.windowStart > WINDOW_MS) {
                return new RateLimitEntry(now, new AtomicInteger(1));
            }
            v.count.incrementAndGet();
            return v;
        });

        return entry.count.get() <= maxRequests;
    }

    private void handleRateLimitExceeded(HttpServletResponse response, String clientIp, String path, String limitType)
            throws IOException {
        totalBlockedRequests.incrementAndGet();
        log.warn("Rate limit exceeded: ip={}, path={}, type={}", clientIp, path, limitType);

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.setHeader("Retry-After", "60");
        response.getWriter().write("{\"error\":\"Trop de requetes. Veuillez reessayer dans une minute.\",\"retryAfter\":60}");
    }

    private int getRateLimit(String path, String method) {
        // Auth endpoints - strictest limits
        if (isAuthEndpoint(path)) {
            return RATE_LIMIT_AUTH;
        }

        // Payment endpoints
        if (path.contains("/payments/") || path.contains("/wallet/")) {
            return RATE_LIMIT_PAYMENT;
        }

        // Booking endpoints
        if (path.contains("/lessons/book") || path.contains("/availabilities")) {
            return RATE_LIMIT_BOOKING;
        }

        // Upload endpoints
        if (path.contains("/upload")) {
            return RATE_LIMIT_UPLOAD;
        }

        // Contact form
        if (path.contains("/contact")) {
            return RATE_LIMIT_CONTACT;
        }

        // Write operations
        if ("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method) || "DELETE".equals(method)) {
            return RATE_LIMIT_API_WRITE;
        }

        // Read operations
        return RATE_LIMIT_API_READ;
    }

    private String getEndpointCategory(String path) {
        if (isAuthEndpoint(path)) return "auth";
        if (path.contains("/payments/") || path.contains("/wallet/")) return "payment";
        if (path.contains("/lessons/")) return "lessons";
        if (path.contains("/upload")) return "upload";
        if (path.contains("/contact")) return "contact";
        if (path.contains("/admin/")) return "admin";
        return "api";
    }

    private boolean isAuthEndpoint(String path) {
        return path.contains("/auth/login") ||
               path.contains("/auth/admin-login") ||
               path.contains("/auth/register") ||
               path.contains("/auth/forgot-password") ||
               path.contains("/auth/reset-password");
    }

    private boolean isExcludedPath(String path) {
        return path.contains("/actuator/health") ||
               path.contains("/actuator/info") ||
               path.equals("/api/health") ||
               path.contains("/uploads/") ||
               path.endsWith(".js") ||
               path.endsWith(".css") ||
               path.endsWith(".ico");
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    private static class RateLimitEntry {
        final long windowStart;
        final AtomicInteger count;

        RateLimitEntry(long windowStart, AtomicInteger count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }

    // Cleanup old entries every 5 minutes
    @Scheduled(fixedRate = 300000)
    public void cleanupOldEntries() {
        long now = System.currentTimeMillis();
        int sizeBefore = requestCounts.size();
        requestCounts.entrySet().removeIf(entry -> now - entry.getValue().windowStart > WINDOW_MS * 2);
        int sizeAfter = requestCounts.size();

        if (sizeBefore > sizeAfter) {
            log.debug("Rate limit cleanup: removed {} entries, {} remaining", sizeBefore - sizeAfter, sizeAfter);
        }
    }

    // Expose metrics
    public long getTotalBlockedRequests() {
        return totalBlockedRequests.get();
    }

    public int getActiveRateLimitEntries() {
        return requestCounts.size();
    }
}
