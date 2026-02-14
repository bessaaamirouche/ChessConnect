package com.chessconnect.repository;

import com.chessconnect.model.Payment;
import com.chessconnect.model.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Modifying;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByPayerIdOrderByCreatedAtDesc(Long payerId);

    List<Payment> findByTeacherIdOrderByCreatedAtDesc(Long teacherId);

    Optional<Payment> findByStripePaymentIntentId(String paymentIntentId);

    Optional<Payment> findByLessonId(Long lessonId);

    @Query("SELECT p FROM Payment p WHERE p.lesson.id = :lessonId AND p.payer.id = :payerId")
    Optional<Payment> findByLessonIdAndPayerId(@Param("lessonId") Long lessonId, @Param("payerId") Long payerId);

    List<Payment> findByPayerIdAndStatus(Long payerId, PaymentStatus status);

    long countByPayerIdAndStatus(Long payerId, PaymentStatus status);

    @Query("SELECT p FROM Payment p WHERE p.teacher.id = :teacherId AND p.status = 'COMPLETED' " +
           "AND p.createdAt >= :startDate AND p.createdAt <= :endDate")
    List<Payment> findCompletedPaymentsForTeacher(
            @Param("teacherId") Long teacherId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT COALESCE(SUM(p.teacherPayoutCents), 0) FROM Payment p " +
           "WHERE p.teacher.id = :teacherId AND p.status = 'COMPLETED'")
    Integer getTotalEarningsForTeacher(@Param("teacherId") Long teacherId);

    @Query("SELECT COALESCE(SUM(p.commissionCents), 0) FROM Payment p WHERE p.status = 'COMPLETED'")
    Integer getTotalPlatformCommissions();

    @Modifying
    void deleteByPayerId(Long payerId);

    @Modifying
    void deleteByTeacherId(Long teacherId);
}
