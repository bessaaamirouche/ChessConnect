package com.chessconnect.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages SSE connections for real-time notifications.
 * Maintains a map of userId -> SseEmitter for pushing events to connected clients.
 * Sends periodic heartbeats to keep connections alive through proxies (nginx).
 *
 * Security features:
 * - Max connections per user (prevents tab flooding)
 * - Global connection limit (prevents DoS)
 * - Automatic cleanup of dead connections
 */
@Service
public class SseConnectionManager {

    private static final Logger log = LoggerFactory.getLogger(SseConnectionManager.class);
    private static final long SSE_TIMEOUT = 30 * 60 * 1000L; // 30 minutes
    private static final long HEARTBEAT_INTERVAL_SECONDS = 20; // Reduced from 25 for better keepalive
    private static final int MAX_GLOBAL_CONNECTIONS = 1000; // Prevent DoS
    private static final int MAX_CONNECTIONS_PER_USER = 3; // Allow a few tabs

    // Map userId -> List of SseEmitters (support multiple tabs)
    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final AtomicInteger totalConnections = new AtomicInteger(0);

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
     * Send heartbeat to all connected clients.
     * Uses a named event with timestamp so clients can verify connection health.
     */
    private void sendHeartbeats() {
        if (emitters.isEmpty()) return;

        int cleanedUp = 0;
        String heartbeatData = "{\"timestamp\":" + Instant.now().toEpochMilli() + "}";

        for (Map.Entry<Long, List<SseEmitter>> entry : emitters.entrySet()) {
            Long userId = entry.getKey();
            List<SseEmitter> userEmitters = entry.getValue();
            List<SseEmitter> deadEmitters = new ArrayList<>();

            for (SseEmitter emitter : userEmitters) {
                try {
                    // Send as named event so client can track heartbeats
                    emitter.send(SseEmitter.event().name("heartbeat").data(heartbeatData));
                } catch (Exception e) {
                    deadEmitters.add(emitter);
                }
            }

            // Clean up dead connections for this user
            if (!deadEmitters.isEmpty()) {
                userEmitters.removeAll(deadEmitters);
                cleanedUp += deadEmitters.size();
                totalConnections.addAndGet(-deadEmitters.size());

                for (SseEmitter dead : deadEmitters) {
                    try { dead.complete(); } catch (Exception ignored) {}
                }

                // Remove user entry if no connections left
                if (userEmitters.isEmpty()) {
                    emitters.remove(userId);
                }
            }
        }

        if (cleanedUp > 0) {
            log.debug("Cleaned up {} dead SSE connections (remaining: {})", cleanedUp, totalConnections.get());
        }
    }

    /**
     * Register a new SSE connection for a user.
     * Supports multiple connections per user (different tabs) with limits.
     *
     * @return SseEmitter or null if limits exceeded
     */
    public SseEmitter createEmitter(Long userId) {
        // Check global connection limit
        if (totalConnections.get() >= MAX_GLOBAL_CONNECTIONS) {
            log.warn("SSE connection rejected for user {}: global limit reached ({})", userId, MAX_GLOBAL_CONNECTIONS);
            return null;
        }

        // Check per-user connection limit
        List<SseEmitter> userEmitters = emitters.computeIfAbsent(userId, k -> new ArrayList<>());
        synchronized (userEmitters) {
            if (userEmitters.size() >= MAX_CONNECTIONS_PER_USER) {
                // Close oldest connection to make room
                SseEmitter oldest = userEmitters.remove(0);
                totalConnections.decrementAndGet();
                try {
                    oldest.send(SseEmitter.event().name("replaced").data("{\"reason\":\"new_connection\"}"));
                    oldest.complete();
                } catch (Exception ignored) {}
                log.debug("Closed oldest SSE connection for user {} to make room", userId);
            }

            SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
            final List<SseEmitter> finalUserEmitters = userEmitters;

            emitter.onCompletion(() -> {
                log.debug("SSE connection completed for user {}", userId);
                removeEmitterFromList(userId, emitter, finalUserEmitters);
            });

            emitter.onTimeout(() -> {
                log.debug("SSE connection timed out for user {}", userId);
                removeEmitterFromList(userId, emitter, finalUserEmitters);
            });

            emitter.onError(ex -> {
                log.debug("SSE connection error for user {}: {}", userId, ex.getMessage());
                removeEmitterFromList(userId, emitter, finalUserEmitters);
            });

            userEmitters.add(emitter);
            int total = totalConnections.incrementAndGet();

            // Send initial connected event
            try {
                emitter.send(SseEmitter.event()
                        .name("connected")
                        .data("{\"userId\":" + userId + ",\"timestamp\":" + Instant.now().toEpochMilli() + "}"));
            } catch (IOException e) {
                log.warn("Failed to send initial connected event to user {}", userId);
            }

            log.info("SSE connection established for user {} (user connections: {}, total: {})",
                    userId, userEmitters.size(), total);

            return emitter;
        }
    }

    private void removeEmitterFromList(Long userId, SseEmitter emitter, List<SseEmitter> userEmitters) {
        synchronized (userEmitters) {
            if (userEmitters.remove(emitter)) {
                totalConnections.decrementAndGet();
                if (userEmitters.isEmpty()) {
                    emitters.remove(userId);
                }
            }
        }
    }

    /**
     * Send an event to a specific user (all their connections).
     */
    public void sendToUser(Long userId, String eventName, Object data) {
        List<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters == null || userEmitters.isEmpty()) {
            log.debug("No SSE connection for user {}", userId);
            return;
        }

        List<SseEmitter> deadEmitters = new ArrayList<>();

        synchronized (userEmitters) {
            for (SseEmitter emitter : userEmitters) {
                try {
                    emitter.send(SseEmitter.event()
                            .name(eventName)
                            .data(data));
                } catch (Exception e) {
                    deadEmitters.add(emitter);
                }
            }

            // Cleanup dead connections
            for (SseEmitter dead : deadEmitters) {
                userEmitters.remove(dead);
                totalConnections.decrementAndGet();
                try { dead.complete(); } catch (Exception ignored) {}
            }

            if (userEmitters.isEmpty()) {
                emitters.remove(userId);
            }
        }

        if (!deadEmitters.isEmpty()) {
            log.debug("Cleaned {} dead connections while sending to user {}", deadEmitters.size(), userId);
        } else {
            log.debug("Sent SSE event '{}' to user {} ({} connections)", eventName, userId, userEmitters.size());
        }
    }

    /**
     * Broadcast an event to all connected users.
     */
    public void broadcast(String eventName, Object data) {
        int sent = 0;
        int failed = 0;

        for (Map.Entry<Long, List<SseEmitter>> entry : emitters.entrySet()) {
            Long userId = entry.getKey();
            List<SseEmitter> userEmitters = entry.getValue();
            List<SseEmitter> deadEmitters = new ArrayList<>();

            synchronized (userEmitters) {
                for (SseEmitter emitter : userEmitters) {
                    try {
                        emitter.send(SseEmitter.event()
                                .name(eventName)
                                .data(data));
                        sent++;
                    } catch (Exception e) {
                        deadEmitters.add(emitter);
                        failed++;
                    }
                }

                for (SseEmitter dead : deadEmitters) {
                    userEmitters.remove(dead);
                    totalConnections.decrementAndGet();
                    try { dead.complete(); } catch (Exception ignored) {}
                }

                if (userEmitters.isEmpty()) {
                    emitters.remove(userId);
                }
            }
        }

        log.debug("Broadcast '{}': sent to {} connections, {} failed", eventName, sent, failed);
    }

    /**
     * Remove and close all emitters for a user.
     */
    public void removeEmitter(Long userId) {
        List<SseEmitter> userEmitters = emitters.remove(userId);
        if (userEmitters != null) {
            synchronized (userEmitters) {
                for (SseEmitter emitter : userEmitters) {
                    totalConnections.decrementAndGet();
                    try { emitter.complete(); } catch (Exception ignored) {}
                }
            }
            log.debug("Removed all SSE connections for user {}", userId);
        }
    }

    /**
     * Check if a user has an active SSE connection.
     */
    public boolean hasConnection(Long userId) {
        List<SseEmitter> userEmitters = emitters.get(userId);
        return userEmitters != null && !userEmitters.isEmpty();
    }

    /**
     * Get the count of active connections (total across all users).
     */
    public int getConnectionCount() {
        return totalConnections.get();
    }

    /**
     * Get the count of connected users.
     */
    public int getConnectedUserCount() {
        return emitters.size();
    }

    /**
     * Get connection statistics for monitoring.
     */
    public Map<String, Object> getStats() {
        return Map.of(
                "totalConnections", totalConnections.get(),
                "connectedUsers", emitters.size(),
                "maxGlobalConnections", MAX_GLOBAL_CONNECTIONS,
                "maxConnectionsPerUser", MAX_CONNECTIONS_PER_USER
        );
    }
}
