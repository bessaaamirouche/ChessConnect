package com.chessconnect.service;

import com.chessconnect.model.UserNotification;
import com.chessconnect.repository.UserNotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationCleanupService {

    private static final Logger log = LoggerFactory.getLogger(NotificationCleanupService.class);

    private static final int MAX_NOTIFICATIONS_PER_USER = 20;
    private static final int UNREAD_RETENTION_DAYS = 15;

    private final UserNotificationRepository notificationRepository;

    public NotificationCleanupService(UserNotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /**
     * Scheduled cleanup task that runs every day at 3:00 AM.
     * - Deletes unread notifications older than 15 days
     * - Enforces max 20 notifications per user
     */
    @Scheduled(cron = "0 0 3 * * *") // Every day at 3:00 AM
    @Transactional
    public void cleanupNotifications() {
        log.info("Starting notification cleanup...");

        int totalDeleted = 0;

        // 1. Delete unread notifications older than 15 days
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(UNREAD_RETENTION_DAYS);
        int expiredDeleted = notificationRepository.deleteUnreadOlderThan(cutoffDate);
        totalDeleted += expiredDeleted;
        log.info("Deleted {} unread notifications older than {} days", expiredDeleted, UNREAD_RETENTION_DAYS);

        // 2. Enforce max notifications per user
        int excessDeleted = enforceMaxNotificationsPerUser();
        totalDeleted += excessDeleted;

        log.info("Notification cleanup completed. Total deleted: {}", totalDeleted);
    }

    /**
     * Enforce the maximum number of notifications per user.
     * Deletes oldest notifications when user exceeds the limit.
     */
    @Transactional
    public int enforceMaxNotificationsPerUser() {
        int totalDeleted = 0;

        // Find users who have more than MAX_NOTIFICATIONS_PER_USER
        List<Long> userIds = notificationRepository.findUserIdsExceedingNotificationLimit(MAX_NOTIFICATIONS_PER_USER);

        for (Long userId : userIds) {
            List<UserNotification> userNotifications = notificationRepository.findByUserIdOrderByCreatedAtDescAll(userId);

            if (userNotifications.size() > MAX_NOTIFICATIONS_PER_USER) {
                // Keep only the first MAX_NOTIFICATIONS_PER_USER (most recent)
                List<UserNotification> toDelete = userNotifications.subList(
                        MAX_NOTIFICATIONS_PER_USER,
                        userNotifications.size()
                );

                notificationRepository.deleteAll(toDelete);
                totalDeleted += toDelete.size();
                log.debug("Deleted {} excess notifications for user {}", toDelete.size(), userId);
            }
        }

        if (totalDeleted > 0) {
            log.info("Deleted {} excess notifications across {} users", totalDeleted, userIds.size());
        }

        return totalDeleted;
    }

    /**
     * Manual cleanup method that can be called from admin endpoints if needed.
     */
    @Transactional
    public CleanupResult runManualCleanup() {
        log.info("Running manual notification cleanup...");

        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(UNREAD_RETENTION_DAYS);
        int expiredDeleted = notificationRepository.deleteUnreadOlderThan(cutoffDate);
        int excessDeleted = enforceMaxNotificationsPerUser();

        return new CleanupResult(expiredDeleted, excessDeleted);
    }

    public record CleanupResult(int expiredDeleted, int excessDeleted) {
        public int total() {
            return expiredDeleted + excessDeleted;
        }
    }
}
