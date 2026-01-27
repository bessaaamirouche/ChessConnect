package com.chessconnect.controller;

import com.chessconnect.model.UserNotification;
import com.chessconnect.security.UserDetailsImpl;
import com.chessconnect.service.UserNotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final UserNotificationService notificationService;

    public NotificationController(UserNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Get all notifications for the current user.
     */
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        Long userId = userDetails.getId();
        List<UserNotification> notifications = notificationService.getNotificationsForUser(userId);
        return ResponseEntity.ok(notifications.stream().map(NotificationResponse::from).toList());
    }

    /**
     * Get unread notifications for the current user.
     */
    @GetMapping("/unread")
    public ResponseEntity<List<NotificationResponse>> getUnreadNotifications(
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        Long userId = userDetails.getId();
        List<UserNotification> notifications = notificationService.getUnreadNotificationsForUser(userId);
        return ResponseEntity.ok(notifications.stream().map(NotificationResponse::from).toList());
    }

    /**
     * Get unread count for the current user.
     */
    @GetMapping("/unread/count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        Long userId = userDetails.getId();
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * Mark a notification as read.
     */
    @PatchMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        Long userId = userDetails.getId();
        notificationService.markAsRead(id, userId);
        return ResponseEntity.ok(Map.of("message", "Notification marked as read"));
    }

    /**
     * Mark all notifications as read.
     */
    @PatchMapping("/read-all")
    public ResponseEntity<?> markAllAsRead(
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        Long userId = userDetails.getId();
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
    }

    /**
     * Delete a notification.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNotification(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        Long userId = userDetails.getId();
        notificationService.deleteNotification(id, userId);
        return ResponseEntity.ok(Map.of("message", "Notification deleted"));
    }

    // Response DTO
    public record NotificationResponse(
            Long id,
            String type,
            String title,
            String message,
            String link,
            boolean isRead,
            String createdAt
    ) {
        public static NotificationResponse from(UserNotification n) {
            return new NotificationResponse(
                    n.getId(),
                    n.getType().name().toLowerCase(),
                    n.getTitle(),
                    n.getMessage(),
                    n.getLink(),
                    Boolean.TRUE.equals(n.getIsRead()),
                    n.getCreatedAt().toString()
            );
        }
    }
}
