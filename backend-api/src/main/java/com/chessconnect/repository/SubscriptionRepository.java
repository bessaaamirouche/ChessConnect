package com.chessconnect.repository;

import com.chessconnect.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByStudentIdAndIsActiveTrue(Long studentId);

    boolean existsByStudentIdAndIsActiveTrue(Long studentId);

    @Query("SELECT s FROM Subscription s WHERE s.student.id = :studentId AND s.isActive = true ORDER BY s.createdAt DESC")
    List<Subscription> findActiveSubscriptionsByStudentId(@Param("studentId") Long studentId);

    List<Subscription> findByStudentIdOrderByCreatedAtDesc(Long studentId);

    List<Subscription> findAllByIsActiveTrue();

    Optional<Subscription> findByStripeSubscriptionId(String stripeSubscriptionId);

    long countByIsActiveTrue();

    @Modifying
    void deleteByStudentId(Long studentId);

    /**
     * Count new subscriptions per day within a date range.
     * Returns List of [date, count] pairs.
     */
    @Query(value = "SELECT DATE(created_at) as date, COUNT(*) as count " +
           "FROM subscriptions WHERE created_at BETWEEN :start AND :end " +
           "GROUP BY DATE(created_at) ORDER BY date", nativeQuery = true)
    List<Object[]> countNewSubscriptionsByDay(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    /**
     * Count cancellations per day within a date range.
     * Returns List of [date, count] pairs.
     */
    @Query(value = "SELECT DATE(cancelled_at) as date, COUNT(*) as count " +
           "FROM subscriptions WHERE cancelled_at IS NOT NULL AND cancelled_at BETWEEN :start AND :end " +
           "GROUP BY DATE(cancelled_at) ORDER BY date", nativeQuery = true)
    List<Object[]> countCancellationsByDay(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    /**
     * Count renewals (subscriptions where user had a previous subscription) per day.
     * Returns List of [date, count] pairs.
     */
    @Query(value = "SELECT DATE(s.created_at) as date, COUNT(*) as count " +
           "FROM subscriptions s " +
           "WHERE s.created_at BETWEEN :start AND :end " +
           "AND EXISTS (SELECT 1 FROM subscriptions prev WHERE prev.student_id = s.student_id AND prev.id < s.id) " +
           "GROUP BY DATE(s.created_at) ORDER BY date", nativeQuery = true)
    List<Object[]> countRenewalsByDay(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
