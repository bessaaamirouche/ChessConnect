package com.chessconnect.service;

import com.chessconnect.model.PushSubscription;
import com.chessconnect.model.User;
import com.chessconnect.repository.PushSubscriptionRepository;
import com.chessconnect.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.apache.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for managing Web Push subscriptions and sending push notifications.
 */
@Service
public class WebPushService {

    private static final Logger log = LoggerFactory.getLogger(WebPushService.class);
    private static final String DEFAULT_ICON = "/assets/icons/icon-192x192.png";

    private final PushService pushService;
    private final PushSubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final SseConnectionManager sseConnectionManager;
    private final ObjectMapper objectMapper;

    public WebPushService(
            PushService pushService,
            PushSubscriptionRepository subscriptionRepository,
            UserRepository userRepository,
            SseConnectionManager sseConnectionManager,
            ObjectMapper objectMapper
    ) {
        this.pushService = pushService;
        this.subscriptionRepository = subscriptionRepository;
        this.userRepository = userRepository;
        this.sseConnectionManager = sseConnectionManager;
        this.objectMapper = objectMapper;
    }

    /**
     * Subscribe a user to push notifications.
     *
     * @param userId    User ID
     * @param endpoint  Push service endpoint
     * @param p256dh    Public key
     * @param auth      Auth secret
     * @param userAgent Browser user agent (for device identification)
     */
    @Transactional
    public void subscribe(Long userId, String endpoint, String p256dh, String auth, String userAgent) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Check if subscription already exists for this endpoint
        Optional<PushSubscription> existing = subscriptionRepository.findByEndpoint(endpoint);
        if (existing.isPresent()) {
            // Update existing subscription
            PushSubscription sub = existing.get();
            sub.setUser(user);
            sub.setP256dh(p256dh);
            sub.setAuth(auth);
            sub.setUserAgent(userAgent);
            sub.setLastUsedAt(LocalDateTime.now());
            subscriptionRepository.save(sub);
            log.info("Updated push subscription for user {} (endpoint: {}...)", userId, endpoint.substring(0, Math.min(50, endpoint.length())));
        } else {
            // Create new subscription
            PushSubscription subscription = new PushSubscription();
            subscription.setUser(user);
            subscription.setEndpoint(endpoint);
            subscription.setP256dh(p256dh);
            subscription.setAuth(auth);
            subscription.setUserAgent(userAgent);
            subscriptionRepository.save(subscription);
            log.info("Created push subscription for user {} (endpoint: {}...)", userId, endpoint.substring(0, Math.min(50, endpoint.length())));
        }
    }

    /**
     * Unsubscribe a push endpoint.
     *
     * @param endpoint Push service endpoint
     */
    @Transactional
    public void unsubscribe(String endpoint) {
        subscriptionRepository.deleteByEndpoint(endpoint);
        log.info("Deleted push subscription (endpoint: {}...)", endpoint.substring(0, Math.min(50, endpoint.length())));
    }

    /**
     * Unsubscribe all push endpoints for a user.
     *
     * @param userId User ID
     */
    @Transactional
    public void unsubscribeAll(Long userId) {
        subscriptionRepository.deleteByUserId(userId);
        log.info("Deleted all push subscriptions for user {}", userId);
    }

    /**
     * Send a push notification to all devices of a user.
     * Only sends if user has push notifications enabled and is not connected via SSE.
     *
     * @param userId User ID
     * @param title  Notification title
     * @param body   Notification body
     * @param link   URL to open when clicking the notification
     */
    @Async
    public void sendToUser(Long userId, String title, String body, String link) {
        sendToUser(userId, title, body, link, DEFAULT_ICON, false);
    }

    /**
     * Send a push notification to all devices of a user.
     *
     * @param userId     User ID
     * @param title      Notification title
     * @param body       Notification body
     * @param link       URL to open when clicking the notification
     * @param icon       Icon URL
     * @param forceSend  If true, send even if user is connected via SSE
     */
    @Async
    public void sendToUser(Long userId, String title, String body, String link, String icon, boolean forceSend) {
        try {
            // Check if user has push notifications enabled
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                log.warn("Cannot send push notification: user {} not found", userId);
                return;
            }

            if (Boolean.FALSE.equals(user.getPushNotificationsEnabled())) {
                log.debug("Push notification skipped for user {}: notifications disabled", userId);
                return;
            }

            // Skip if user is connected via SSE (they'll get real-time notification)
            if (!forceSend && sseConnectionManager.hasConnection(userId)) {
                log.debug("Push notification skipped for user {}: connected via SSE", userId);
                return;
            }

            // Get all subscriptions for the user
            List<PushSubscription> subscriptions = subscriptionRepository.findByUserId(userId);
            if (subscriptions.isEmpty()) {
                log.debug("No push subscriptions for user {}", userId);
                return;
            }

            // Prepare payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("title", title);
            payload.put("body", body);
            payload.put("link", link);
            payload.put("icon", icon != null ? icon : DEFAULT_ICON);

            String payloadJson = objectMapper.writeValueAsString(payload);

            // Send to all subscriptions
            for (PushSubscription sub : subscriptions) {
                try {
                    sendPushNotification(sub, payloadJson);
                    // Update last used timestamp
                    sub.setLastUsedAt(LocalDateTime.now());
                    subscriptionRepository.save(sub);
                } catch (Exception e) {
                    handlePushError(sub, e);
                }
            }

            log.info("Sent push notification to user {} ({} devices)", userId, subscriptions.size());
        } catch (Exception e) {
            log.error("Failed to send push notification to user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * Send push notifications to multiple users.
     *
     * @param userIds User IDs
     * @param title   Notification title
     * @param body    Notification body
     * @param link    URL to open when clicking the notification
     */
    @Async
    public void sendToUsers(List<Long> userIds, String title, String body, String link) {
        for (Long userId : userIds) {
            sendToUser(userId, title, body, link);
        }
    }

    /**
     * Send a raw push notification to a subscription.
     */
    private void sendPushNotification(PushSubscription sub, String payload) throws Exception {
        Subscription subscription = new Subscription(
                sub.getEndpoint(),
                new Subscription.Keys(sub.getP256dh(), sub.getAuth())
        );

        Notification notification = new Notification(subscription, payload);
        HttpResponse response = pushService.send(notification);

        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode >= 400) {
            throw new RuntimeException("Push service returned status " + statusCode);
        }
    }

    /**
     * Handle errors when sending push notifications.
     * Removes expired/invalid subscriptions.
     */
    private void handlePushError(PushSubscription sub, Exception e) {
        String message = e.getMessage();
        log.warn("Failed to send push to subscription {}: {}", sub.getId(), message);

        // If the subscription is invalid (410 Gone or 404 Not Found), remove it
        if (message != null && (message.contains("410") || message.contains("404"))) {
            log.info("Removing expired push subscription {}", sub.getId());
            subscriptionRepository.delete(sub);
        }
    }

    /**
     * Check if a user has any push subscriptions.
     */
    public boolean hasSubscriptions(Long userId) {
        return subscriptionRepository.countByUserId(userId) > 0;
    }

    /**
     * Get subscription count for a user.
     */
    public long getSubscriptionCount(Long userId) {
        return subscriptionRepository.countByUserId(userId);
    }
}
