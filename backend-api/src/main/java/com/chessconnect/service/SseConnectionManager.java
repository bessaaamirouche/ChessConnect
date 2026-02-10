package com.chessconnect.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manages SSE connections for real-time notifications.
 * Maintains a map of userId -> SseEmitter for pushing events to connected clients.
 * Sends periodic heartbeats to keep connections alive through proxies (nginx).
 */
@Service
public class SseConnectionManager {

    private static final Logger log = LoggerFactory.getLogger(SseConnectionManager.class);
    private static final long SSE_TIMEOUT = 30 * 60 * 1000L; // 30 minutes
    private static final long HEARTBEAT_INTERVAL_SECONDS = 25;

    // Map userId -> SseEmitter
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    private ScheduledExecutorService heartbeatScheduler;

    @PostConstruct
    public void init() {
        heartbeatScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "sse-heartbeat");
            t.setDaemon(true);
            return t;
        });
        heartbeatScheduler.scheduleAtFixedRate(
                this::sendHeartbeats,
                HEARTBEAT_INTERVAL_SECONDS,
                HEARTBEAT_INTERVAL_SECONDS,
                TimeUnit.SECONDS
        );
        log.info("SSE heartbeat scheduler started (interval: {}s)", HEARTBEAT_INTERVAL_SECONDS);
    }

    @PreDestroy
    public void destroy() {
        if (heartbeatScheduler != null) {
            heartbeatScheduler.shutdown();
        }
        emitters.values().forEach(emitter -> {
            try { emitter.complete(); } catch (Exception ignored) {}
        });
        emitters.clear();
    }

    /**
     * Send heartbeat comment to all connected clients.
     * SSE comments (lines starting with ':') keep the connection alive
     * through nginx proxy without triggering client-side event handlers.
     */
    private void sendHeartbeats() {
        if (emitters.isEmpty()) return;

        List<Long> deadEmitters = new ArrayList<>();

        emitters.forEach((userId, emitter) -> {
            try {
                emitter.send(SseEmitter.event().comment("heartbeat"));
            } catch (Exception e) {
                deadEmitters.add(userId);
            }
        });

        // Clean up dead connections
        for (Long userId : deadEmitters) {
            log.debug("Removing dead SSE connection for user {} (heartbeat failed)", userId);
            SseEmitter removed = emitters.remove(userId);
            if (removed != null) {
                try { removed.complete(); } catch (Exception ignored) {}
            }
        }

        if (!deadEmitters.isEmpty()) {
            log.debug("Cleaned up {} dead SSE connections (remaining: {})", deadEmitters.size(), emitters.size());
        }
    }

    /**
     * Register a new SSE connection for a user.
     * Replaces any existing connection for that user.
     */
    public SseEmitter createEmitter(Long userId) {
        // Close existing emitter if any
        SseEmitter oldEmitter = emitters.remove(userId);
        if (oldEmitter != null) {
            try { oldEmitter.complete(); } catch (Exception ignored) {}
        }

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        emitter.onCompletion(() -> {
            log.debug("SSE connection completed for user {}", userId);
            emitters.remove(userId, emitter);
        });

        emitter.onTimeout(() -> {
            log.debug("SSE connection timed out for user {}", userId);
            emitters.remove(userId, emitter);
        });

        emitter.onError(ex -> {
            log.debug("SSE connection error for user {}: {}", userId, ex.getMessage());
            emitters.remove(userId, emitter);
        });

        emitters.put(userId, emitter);
        log.info("SSE connection established for user {} (total connections: {})", userId, emitters.size());

        return emitter;
    }

    /**
     * Send an event to a specific user.
     */
    public void sendToUser(Long userId, String eventName, Object data) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) {
            log.debug("No SSE connection for user {}", userId);
            return;
        }

        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data));
            log.debug("Sent SSE event '{}' to user {}", eventName, userId);
        } catch (Exception e) {
            log.warn("Failed to send SSE event to user {}: {}", userId, e.getMessage());
            emitters.remove(userId, emitter);
            try { emitter.complete(); } catch (Exception ignored) {}
        }
    }

    /**
     * Broadcast an event to all connected users.
     */
    public void broadcast(String eventName, Object data) {
        List<Long> deadEmitters = new ArrayList<>();

        emitters.forEach((userId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(data));
            } catch (Exception e) {
                log.debug("Failed to broadcast to user {}", userId);
                deadEmitters.add(userId);
            }
        });

        for (Long userId : deadEmitters) {
            SseEmitter removed = emitters.remove(userId);
            if (removed != null) {
                try { removed.complete(); } catch (Exception ignored) {}
            }
        }
    }

    /**
     * Remove and close an emitter for a user.
     */
    public void removeEmitter(Long userId) {
        SseEmitter emitter = emitters.remove(userId);
        if (emitter != null) {
            try {
                emitter.complete();
            } catch (Exception e) {
                log.debug("Error completing emitter for user {}", userId);
            }
        }
    }

    /**
     * Check if a user has an active SSE connection.
     */
    public boolean hasConnection(Long userId) {
        return emitters.containsKey(userId);
    }

    /**
     * Get the count of active connections.
     */
    public int getConnectionCount() {
        return emitters.size();
    }
}
