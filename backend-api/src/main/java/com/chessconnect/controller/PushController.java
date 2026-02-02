package com.chessconnect.controller;

import com.chessconnect.config.WebPushConfig;
import com.chessconnect.dto.PushSubscriptionRequest;
import com.chessconnect.model.User;
import com.chessconnect.repository.UserRepository;
import com.chessconnect.service.WebPushService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for Web Push notifications.
 */
@RestController
@RequestMapping("/push")
public class PushController {

    private static final Logger log = LoggerFactory.getLogger(PushController.class);

    private final WebPushConfig webPushConfig;
    private final WebPushService webPushService;
    private final UserRepository userRepository;

    public PushController(
            WebPushConfig webPushConfig,
            WebPushService webPushService,
            UserRepository userRepository
    ) {
        this.webPushConfig = webPushConfig;
        this.webPushService = webPushService;
        this.userRepository = userRepository;
    }

    /**
     * Get the VAPID public key for client subscription.
     * This endpoint is public (no authentication required).
     */
    @GetMapping("/vapid-key")
    public ResponseEntity<Map<String, String>> getVapidKey() {
        return ResponseEntity.ok(Map.of("publicKey", webPushConfig.getPublicKey()));
    }

    /**
     * Subscribe to push notifications.
     */
    @PostMapping("/subscribe")
    public ResponseEntity<Map<String, Object>> subscribe(
            @Valid @RequestBody PushSubscriptionRequest request,
            Authentication authentication
    ) {
        Long userId = getUserId(authentication);

        webPushService.subscribe(
                userId,
                request.getEndpoint(),
                request.getP256dh(),
                request.getAuth(),
                request.getUserAgent()
        );

        log.info("User {} subscribed to push notifications", userId);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Subscription registered successfully"
        ));
    }

    /**
     * Unsubscribe from push notifications for a specific endpoint.
     */
    @PostMapping("/unsubscribe")
    public ResponseEntity<Map<String, Object>> unsubscribe(
            @RequestBody Map<String, String> request,
            Authentication authentication
    ) {
        String endpoint = request.get("endpoint");
        if (endpoint == null || endpoint.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Endpoint is required"
            ));
        }

        webPushService.unsubscribe(endpoint);

        Long userId = getUserId(authentication);
        log.info("User {} unsubscribed from push notifications", userId);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Subscription removed successfully"
        ));
    }

    /**
     * Get push notification status for the current user.
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus(Authentication authentication) {
        Long userId = getUserId(authentication);
        User user = userRepository.findById(userId).orElseThrow();

        return ResponseEntity.ok(Map.of(
                "enabled", Boolean.TRUE.equals(user.getPushNotificationsEnabled()),
                "subscriptionCount", webPushService.getSubscriptionCount(userId),
                "hasSubscriptions", webPushService.hasSubscriptions(userId)
        ));
    }

    /**
     * Update push notification preference.
     */
    @PatchMapping("/preference")
    public ResponseEntity<Map<String, Object>> updatePreference(
            @RequestBody Map<String, Boolean> request,
            Authentication authentication
    ) {
        Boolean enabled = request.get("enabled");
        if (enabled == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "enabled field is required"
            ));
        }

        Long userId = getUserId(authentication);
        User user = userRepository.findById(userId).orElseThrow();
        user.setPushNotificationsEnabled(enabled);
        userRepository.save(user);

        log.info("User {} {} push notifications", userId, enabled ? "enabled" : "disabled");

        return ResponseEntity.ok(Map.of(
                "success", true,
                "enabled", enabled
        ));
    }

    private Long getUserId(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return user.getId();
    }
}
