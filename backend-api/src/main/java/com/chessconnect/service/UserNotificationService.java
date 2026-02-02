package com.chessconnect.service;

import com.chessconnect.event.NotificationEvent;
import com.chessconnect.event.payload.BackendNotificationPayload;
import com.chessconnect.model.User;
import com.chessconnect.model.UserNotification;
import com.chessconnect.model.enums.NotificationType;
import com.chessconnect.repository.UserNotificationRepository;
import com.chessconnect.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class UserNotificationService {

    private static final Logger log = LoggerFactory.getLogger(UserNotificationService.class);
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final UserNotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public UserNotificationService(
            UserNotificationRepository notificationRepository,
            UserRepository userRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Create a notification for a user.
     */
    @Transactional
    public UserNotification createNotification(Long userId, NotificationType type, String title, String message, String link) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        UserNotification notification = new UserNotification(user, type, title, message, link);
        notification = notificationRepository.save(notification);

        log.info("Created notification for user {}: {} - {}", userId, title, message);

        // Publish SSE event for real-time notification
        publishNotificationEvent(notification);

        return notification;
    }

    /**
     * Publish SSE event for a new notification.
     */
    private void publishNotificationEvent(UserNotification notification) {
        try {
            BackendNotificationPayload payload = new BackendNotificationPayload(
                    notification.getId(),
                    notification.getType().name(),
                    notification.getTitle(),
                    notification.getMessage(),
                    notification.getLink(),
                    notification.getCreatedAt().format(ISO_FORMATTER)
            );

            eventPublisher.publishEvent(new NotificationEvent(
                    this,
                    NotificationEvent.EventType.NOTIFICATION_CREATED,
                    notification.getUser().getId(),
                    payload
            ));
        } catch (Exception e) {
            log.warn("Failed to publish SSE notification event: {}", e.getMessage());
        }
    }

    /**
     * Create a notification without a link.
     */
    @Transactional
    public UserNotification createNotification(Long userId, NotificationType type, String title, String message) {
        return createNotification(userId, type, title, message, null);
    }

    /**
     * Get all notifications for a user (newest first).
     */
    public List<UserNotification> getNotificationsForUser(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Get unread notifications for a user.
     */
    public List<UserNotification> getUnreadNotificationsForUser(Long userId) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
    }

    /**
     * Get unread count for a user.
     */
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    /**
     * Mark a notification as read (deletes it immediately).
     */
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        UserNotification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));

        if (!notification.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Not authorized to update this notification");
        }

        // Delete immediately when read
        notificationRepository.delete(notification);
        log.debug("Deleted read notification {} for user {}", notificationId, userId);
    }

    /**
     * Mark all notifications as read for a user (deletes them all).
     */
    @Transactional
    public void markAllAsRead(Long userId) {
        List<UserNotification> notifications = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
        notificationRepository.deleteAll(notifications);
        log.info("Deleted {} read notifications for user {}", notifications.size(), userId);
    }

    /**
     * Delete a notification.
     */
    @Transactional
    public void deleteNotification(Long notificationId, Long userId) {
        UserNotification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));

        if (!notification.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Not authorized to delete this notification");
        }

        notificationRepository.delete(notification);
    }

    // ============= Convenience methods for common notifications =============

    /**
     * Notify user about a refund.
     */
    public void notifyRefund(Long userId, int amountCents, String reason) {
        String amountFormatted = String.format("%.2f", amountCents / 100.0);
        createNotification(
                userId,
                NotificationType.REFUND,
                "Remboursement effectue",
                String.format("%s EUR ont ete credites sur votre portefeuille. %s", amountFormatted, reason),
                "/wallet"
        );
    }

    /**
     * Notify user about lesson confirmation.
     */
    public void notifyLessonConfirmed(Long userId, String teacherName, String dateTime) {
        createNotification(
                userId,
                NotificationType.LESSON_CONFIRMED,
                "Cours confirme",
                String.format("Votre cours avec %s le %s a ete confirme.", teacherName, dateTime),
                "/lessons"
        );
    }

    /**
     * Notify user about lesson cancellation.
     */
    public void notifyLessonCancelled(Long userId, String otherPartyName, String dateTime, String reason) {
        createNotification(
                userId,
                NotificationType.LESSON_CANCELLED,
                "Cours annule",
                String.format("Le cours avec %s prevu le %s a ete annule. %s", otherPartyName, dateTime, reason != null ? reason : ""),
                "/lessons"
        );
    }

    /**
     * Notify teacher about new booking.
     */
    public void notifyNewBooking(Long teacherId, String studentName, String dateTime) {
        createNotification(
                teacherId,
                NotificationType.NEW_BOOKING,
                "Nouvelle reservation",
                String.format("%s a reserve un cours le %s. Confirmez-le dans votre espace.", studentName, dateTime),
                "/lessons"
        );
    }

    /**
     * Notify teacher about pending course validation.
     * Only creates notification if one doesn't already exist for this teacher/student pair.
     */
    public void notifyPendingValidation(Long teacherId, Long studentId, String studentName) {
        String link = "/lessons?openStudentProfile=" + studentId;

        // Check if notification already exists
        if (notificationRepository.existsByUserIdAndTypeAndLinkAndIsReadFalse(
                teacherId, NotificationType.PENDING_VALIDATION, link)) {
            log.debug("Pending validation notification already exists for teacher {} and student {}", teacherId, studentId);
            return;
        }

        createNotification(
                teacherId,
                NotificationType.PENDING_VALIDATION,
                "Validation en attente",
                String.format("N'oubliez pas de valider les cours de %s", studentName),
                link
        );
    }
}
