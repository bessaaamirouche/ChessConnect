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

    // Rate limits per minute by endpoint category (increased for high traffic)
    private static final int RATE_LIMIT_AUTH = 30;          // Login, register, password reset
    private static final int RATE_LIMIT_PAYMENT = 120;      // Payment operations
    private static final int RATE_LIMIT_BOOKING = 200;      // Lesson booking
    private static final int RATE_LIMIT_AVAILABILITY = 300; // Availabilities (coaches create many slots)
    private static final int RATE_LIMIT_UPLOAD = 30;        // File uploads
    private static final int RATE_LIMIT_CONTACT = 15;       // Contact form
    private static final int RATE_LIMIT_API_WRITE = 300;    // POST/PUT/DELETE general
    private static final int RATE_LIMIT_API_READ = 600;     // GET requests
    private static final int RATE_LIMIT_GLOBAL = 1500;      // Global per IP (high traffic)

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

        // Payment status checks (GET) - higher limit for polling
        if ("GET".equals(method) && (path.contains("/payments/subscription") || path.contains("/wallet/balance"))) {
            return RATE_LIMIT_API_READ;
        }

        // Payment endpoints (POST/etc) - strict limit
        if (path.contains("/payments/") || path.contains("/wallet/")) {
            return RATE_LIMIT_PAYMENT;
        }

        // Availability endpoints (coaches create many slots at once)
        if (path.contains("/availabilities")) {
            return RATE_LIMIT_AVAILABILITY;
        }

        // Booking endpoints
        if (path.contains("/lessons/book")) {
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
        // Separate category for status checks (higher limits)
        if (path.equals("/api/payments/subscription") || path.contains("/wallet/balance")) return "status";
        if (path.contains("/payments/") || path.contains("/wallet/")) return "payment";
        if (path.contains("/availabilities")) return "availability"; // Separate category for high volume
        if (path.contains("/lessons/")) return "lessons";
        if (path.contains("/upload")) return "upload";
        if (path.contains("/contact")) return "contact";
        if (path.contains("/admin/")) return "admin";
        if (path.contains("/notifications")) return "notifications"; // Polling endpoint
        if (path.contains("/teachers")) return "teachers"; // Often polled
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

    /**
     * Get client IP address with protection against header spoofing.
     * Only trusts X-Forwarded-For from known proxies (localhost, Docker network).
     * For direct connections, uses remoteAddr.
     */
    private String getClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();

        // Only trust proxy headers if request comes from trusted proxy (localhost, Docker network)
        if (isTrustedProxy(remoteAddr)) {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                // Take the rightmost IP that's not a trusted proxy (closest to client)
                String[] ips = xForwardedFor.split(",");
                for (int i = ips.length - 1; i >= 0; i--) {
                    String ip = ips[i].trim();
                    if (!isTrustedProxy(ip) && isValidIpAddress(ip)) {
                        return ip;
                    }
                }
                // If all are trusted proxies, take the first one
                String firstIp = ips[0].trim();
                if (isValidIpAddress(firstIp)) {
                    return firstIp;
                }
            }
            String xRealIp = request.getHeader("X-Real-IP");
            if (xRealIp != null && !xRealIp.isEmpty() && isValidIpAddress(xRealIp)) {
                return xRealIp;
            }
        }

        return remoteAddr;
    }

    /**
     * Check if IP is from a trusted proxy (localhost, Docker networks, private networks).
     */
    private boolean isTrustedProxy(String ip) {
        if (ip == null) return false;
        return ip.equals("127.0.0.1") ||
               ip.equals("::1") ||
               ip.startsWith("10.") ||           // Docker default bridge
               ip.startsWith("172.16.") ||       // Docker custom networks
               ip.startsWith("172.17.") ||       // Docker default network
               ip.startsWith("172.18.") ||
               ip.startsWith("172.19.") ||
               ip.startsWith("172.20.") ||
               ip.startsWith("172.21.") ||
               ip.startsWith("172.22.") ||
               ip.startsWith("172.23.") ||
               ip.startsWith("172.24.") ||
               ip.startsWith("172.25.") ||
               ip.startsWith("172.26.") ||
               ip.startsWith("172.27.") ||
               ip.startsWith("172.28.") ||
               ip.startsWith("172.29.") ||
               ip.startsWith("172.30.") ||
               ip.startsWith("172.31.") ||
               ip.startsWith("192.168.");        // Local network
    }

    /**
     * Basic IP address validation to prevent malicious input.
     */
    private boolean isValidIpAddress(String ip) {
        if (ip == null || ip.isEmpty() || ip.length() > 45) return false;
        // Basic regex for IPv4 and IPv6
        return ip.matches("^[0-9a-fA-F.:]+$") &&
               !ip.contains("..") &&
               !ip.contains(":::");
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
