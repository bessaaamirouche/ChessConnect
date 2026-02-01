package com.chessconnect.repository;

import com.chessconnect.model.UserNotification;
import com.chessconnect.model.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserNotificationRepository extends JpaRepository<UserNotification, Long> {

    List<UserNotification> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<UserNotification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndIsReadFalse(Long userId);

    long countByUserId(Long userId);

    @Modifying
    @Query("UPDATE UserNotification n SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP WHERE n.user.id = :userId AND n.isRead = false")
    void markAllAsReadForUser(@Param("userId") Long userId);

    @Modifying
    void deleteByUserId(Long userId);

    // Cleanup methods
    @Modifying
    @Query("DELETE FROM UserNotification n WHERE n.isRead = true")
    int deleteAllReadNotifications();

    @Modifying
    @Query("DELETE FROM UserNotification n WHERE n.isRead = false AND n.createdAt < :cutoffDate")
    int deleteUnreadOlderThan(@Param("cutoffDate") java.time.LocalDateTime cutoffDate);

    @Query("SELECT DISTINCT n.user.id FROM UserNotification n GROUP BY n.user.id HAVING COUNT(n) > :maxCount")
    List<Long> findUserIdsExceedingNotificationLimit(@Param("maxCount") long maxCount);

    @Query("SELECT n FROM UserNotification n WHERE n.user.id = :userId ORDER BY n.createdAt DESC")
    List<UserNotification> findByUserIdOrderByCreatedAtDescAll(@Param("userId") Long userId);

    // Check if a notification of this type and link already exists for the user
    boolean existsByUserIdAndTypeAndLinkAndIsReadFalse(Long userId, NotificationType type, String link);
}
