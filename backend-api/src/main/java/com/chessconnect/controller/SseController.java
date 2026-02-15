package com.chessconnect.controller;

import com.chessconnect.security.UserDetailsImpl;
import com.chessconnect.service.SseConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

/**
 * SSE endpoint for real-time notifications.
 * Clients connect to /api/notifications/stream to receive push notifications.
 */
@RestController
@RequestMapping("/notifications")
public class SseController {

    private static final Logger log = LoggerFactory.getLogger(SseController.class);

    private final SseConnectionManager connectionManager;

    public SseController(SseConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    /**
     * SSE endpoint for real-time notifications.
     * Auth is handled via HttpOnly cookie (withCredentials: true on client).
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamNotifications(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null) {
            throw new IllegalStateException("Authentication required for SSE stream");
        }

        Long userId = userDetails.getId();
        log.info("User {} connecting to SSE stream", userId);

        SseEmitter emitter = connectionManager.createEmitter(userId);

        // Handle connection limit exceeded
        if (emitter == null) {
            log.warn("SSE connection rejected for user {} - limits exceeded", userId);
            SseEmitter errorEmitter = new SseEmitter(0L);
            try {
                errorEmitter.send(SseEmitter.event()
                        .name("error")
                        .data(Map.of("error", "connection_limit_exceeded")));
                errorEmitter.complete();
            } catch (IOException ignored) {}
            return errorEmitter;
        }

        // Initial connected event is sent by the connection manager
        return emitter;
    }

    /**
     * Get SSE connection statistics (for monitoring).
     */
    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        return connectionManager.getStats();
    }
}
