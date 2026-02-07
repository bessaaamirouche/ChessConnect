package com.chessconnect.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class MaintenanceFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(MaintenanceFilter.class);

    @Value("${app.maintenance.enabled:false}")
    private boolean maintenanceEnabled;

    // Endpoints blocked during maintenance (path after /api context)
    private static final List<BlockedEndpoint> BLOCKED_ENDPOINTS = List.of(
        new BlockedEndpoint("POST", "/api/auth/register"),
        new BlockedEndpoint("POST", "/api/payments/checkout"),
        new BlockedEndpoint("POST", "/api/payments/subscription"),
        new BlockedEndpoint("POST", "/api/lessons/book"),
        new BlockedEndpoint("POST", "/api/lessons/free-trial/book"),
        new BlockedEndpoint("POST", "/api/wallet/credit"),
        new BlockedEndpoint("POST", "/api/wallet/topup"),
        new BlockedEndpoint("POST", "/api/wallet/book-with-credit"),
        new BlockedEndpoint("POST", "/api/availabilities")
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (!maintenanceEnabled) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        String method = request.getMethod();

        if (isBlocked(method, path)) {
            log.info("Maintenance mode: blocked {} {}", method, path);
            response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Service en maintenance\",\"maintenance\":true}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isBlocked(String method, String path) {
        for (BlockedEndpoint endpoint : BLOCKED_ENDPOINTS) {
            if (endpoint.method.equals(method) && path.startsWith(endpoint.pathPrefix)) {
                return true;
            }
        }
        return false;
    }

    private record BlockedEndpoint(String method, String pathPrefix) {}
}
