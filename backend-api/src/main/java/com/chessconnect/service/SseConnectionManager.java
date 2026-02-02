package com.chessconnect.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages SSE connections for real-time notifications.
 * Maintains a map of userId -> SseEmitter for pushing events to connected clients.
 */
@Service
public class SseConnectionManager {

    private static final Logger log = LoggerFactory.getLogger(SseConnectionManager.class);
    private static final long SSE_TIMEOUT = 30 * 60 * 1000L; // 30 minutes

    // Map userId -> SseEmitter
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * Register a new SSE connection for a user.
     * Replaces any existing connection for that user.
     */
    public SseEmitter createEmitter(Long userId) {
        // Close existing emitter if any
        removeEmitter(userId);

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
        } catch (IOException e) {
            log.warn("Failed to send SSE event to user {}: {}", userId, e.getMessage());
            removeEmitter(userId);
        }
    }

    /**
     * Broadcast an event to all connected users.
     */
    public void broadcast(String eventName, Object data) {
        emitters.forEach((userId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(data));
            } catch (IOException e) {
                log.debug("Failed to broadcast to user {}", userId);
                removeEmitter(userId);
            }
        });
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
