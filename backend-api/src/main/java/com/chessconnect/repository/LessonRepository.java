package com.chessconnect.repository;

import com.chessconnect.model.Lesson;
import com.chessconnect.model.User;
import com.chessconnect.model.enums.LessonStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Modifying;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Collection;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {

    List<Lesson> findByStudentIdOrderByScheduledAtDesc(Long studentId);

    List<Lesson> findByTeacherIdOrderByScheduledAtDesc(Long teacherId);

    List<Lesson> findByTeacherIdAndStatus(Long teacherId, LessonStatus status);

    @Query("SELECT l FROM Lesson l WHERE l.teacher.id = :teacherId " +
           "AND l.scheduledAt BETWEEN :start AND :end")
    List<Lesson> findTeacherLessonsBetween(
            @Param("teacherId") Long teacherId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("SELECT l FROM Lesson l WHERE l.student.id = :studentId " +
           "AND l.scheduledAt BETWEEN :start AND :end")
    List<Lesson> findStudentLessonsBetween(
            @Param("studentId") Long studentId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query(value = "SELECT * FROM lessons l WHERE l.teacher_id = :teacherId " +
           "AND (l.scheduled_at + (l.duration_minutes * INTERVAL '1 minute')) >= :dateTime " +
           "AND l.status IN ('PENDING', 'CONFIRMED', 'CANCELLED') " +
           "ORDER BY l.scheduled_at ASC", nativeQuery = true)
    List<Lesson> findUpcomingLessonsForTeacher(
            @Param("teacherId") Long teacherId,
            @Param("dateTime") LocalDateTime dateTime
    );

    @Query(value = "SELECT * FROM lessons l WHERE l.student_id = :studentId " +
           "AND (l.scheduled_at + (l.duration_minutes * INTERVAL '1 minute')) >= :dateTime " +
           "AND l.status IN ('PENDING', 'CONFIRMED', 'CANCELLED') " +
           "ORDER BY l.scheduled_at ASC", nativeQuery = true)
    List<Lesson> findUpcomingLessonsForStudent(
            @Param("studentId") Long studentId,
            @Param("dateTime") LocalDateTime dateTime
    );

    @Query("SELECT COALESCE(SUM(l.teacherEarningsCents), 0) FROM Lesson l " +
           "WHERE l.teacher.id = :teacherId AND l.status = 'COMPLETED'")
    Long calculateTotalEarningsForTeacher(@Param("teacherId") Long teacherId);

    List<Lesson> findByTeacherIdAndScheduledAtBetween(Long teacherId, LocalDateTime start, LocalDateTime end);

    // For auto-cancellation of unconfirmed lessons
    List<Lesson> findByStatusAndCreatedAtBefore(LessonStatus status, LocalDateTime dateTime);

    // For auto-completion of lessons after 45 minutes
    List<Lesson> findByStatusAndScheduledAtBefore(LessonStatus status, LocalDateTime dateTime);

    // For migration: find completed lessons that haven't been credited yet
    @Query("SELECT l FROM Lesson l WHERE l.status = 'COMPLETED' AND (l.earningsCredited IS NULL OR l.earningsCredited = false)")
    List<Lesson> findCompletedLessonsNotCredited();

    // For lesson reminders
    @Query("SELECT l FROM Lesson l WHERE l.status = :status AND l.scheduledAt BETWEEN :start AND :end AND (l.reminderSent IS NULL OR l.reminderSent = false)")
    List<Lesson> findLessonsForReminder(
            @Param("status") LessonStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // Admin queries
    @Query("SELECT COUNT(l) FROM Lesson l WHERE l.student.id = :userId OR l.teacher.id = :userId")
    Long countByStudentIdOrTeacherId(@Param("userId") Long userId);

    @Query("SELECT COUNT(l) FROM Lesson l WHERE l.scheduledAt BETWEEN :start AND :end")
    Long countByScheduledAtBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Modifying
    void deleteByStudentId(Long studentId);

    @Modifying
    void deleteByTeacherId(Long teacherId);

    // Teacher profile stats
    @Query("SELECT COUNT(l) FROM Lesson l WHERE l.teacher.id = :teacherId AND l.status = 'COMPLETED'")
    Integer countCompletedLessonsByTeacherId(@Param("teacherId") Long teacherId);

    @Query("SELECT COUNT(DISTINCT l.student.id) FROM Lesson l WHERE l.teacher.id = :teacherId AND l.status = 'COMPLETED'")
    Integer countDistinctStudentsByTeacherId(@Param("teacherId") Long teacherId);

    // Optimized admin queries - avoid findAll() + in-memory filtering
    @Query("SELECT l FROM Lesson l WHERE l.status IN :statuses AND l.scheduledAt > :after ORDER BY l.scheduledAt ASC")
    List<Lesson> findByStatusInAndScheduledAtAfter(@Param("statuses") List<LessonStatus> statuses, @Param("after") LocalDateTime after);

    @Query("SELECT l FROM Lesson l WHERE l.status = :status ORDER BY l.scheduledAt DESC")
    List<Lesson> findByStatusOrderByScheduledAtDesc(@Param("status") LessonStatus status);

    // Accounting aggregates - calculate in database, not in memory
    @Query("SELECT COALESCE(SUM(l.priceCents), 0) FROM Lesson l WHERE l.status = 'COMPLETED'")
    Long sumPriceCentsCompleted();

    @Query("SELECT COALESCE(SUM(l.commissionCents), 0) FROM Lesson l WHERE l.status = 'COMPLETED'")
    Long sumCommissionCentsCompleted();

    @Query("SELECT COALESCE(SUM(l.teacherEarningsCents), 0) FROM Lesson l WHERE l.status = 'COMPLETED'")
    Long sumTeacherEarningsCentsCompleted();

    @Query("SELECT COALESCE(SUM(l.refundedAmountCents), 0) FROM Lesson l WHERE l.status = 'CANCELLED'")
    Long sumRefundedAmountCentsCancelled();

    @Query("SELECT COUNT(l) FROM Lesson l WHERE l.status = 'COMPLETED'")
    Long countCompleted();

    @Query("SELECT COUNT(l) FROM Lesson l WHERE l.status = 'CANCELLED'")
    Long countCancelled();

    // Revenue this month
    @Query("SELECT COALESCE(SUM(l.priceCents), 0) FROM Lesson l WHERE l.status = 'COMPLETED' AND l.scheduledAt BETWEEN :start AND :end")
    Long sumPriceCentsCompletedBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // Check for active lessons for a user (deleteUser check)
    @Query("SELECT COUNT(l) FROM Lesson l WHERE (l.student.id = :userId OR l.teacher.id = :userId) AND l.status IN ('PENDING', 'CONFIRMED')")
    Long countActiveLessonsByUserId(@Param("userId") Long userId);

    // Teacher current month earnings
    @Query("SELECT COALESCE(SUM(l.teacherEarningsCents), 0) FROM Lesson l WHERE l.teacher.id = :teacherId AND l.status = 'COMPLETED' AND l.scheduledAt BETWEEN :start AND :end")
    Integer sumTeacherEarningsBetween(@Param("teacherId") Long teacherId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(l) FROM Lesson l WHERE l.teacher.id = :teacherId AND l.status = 'COMPLETED' AND l.scheduledAt BETWEEN :start AND :end")
    Integer countTeacherCompletedBetween(@Param("teacherId") Long teacherId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // For balance recalculation: find all credited lessons for a teacher
    @Query("SELECT l FROM Lesson l WHERE l.teacher.id = :teacherId AND l.earningsCredited = true")
    List<Lesson> findByTeacherIdAndEarningsCreditedTrue(@Param("teacherId") Long teacherId);

    // Admin: Get all past lessons (COMPLETED + CANCELLED) ordered by date desc
    @Query("SELECT l FROM Lesson l WHERE l.status IN ('COMPLETED', 'CANCELLED') ORDER BY l.scheduledAt DESC")
    List<Lesson> findAllPastLessons();

    // Admin: Get all lessons (all statuses) ordered by date desc
    @Query("SELECT l FROM Lesson l ORDER BY l.scheduledAt DESC")
    List<Lesson> findAllOrderByScheduledAtDesc();

    // Batch query for lesson counts by user - fixes N+1 in AdminService.getUsers()
    @Query("SELECT u.id, COUNT(l) FROM User u LEFT JOIN Lesson l ON (l.student.id = u.id OR l.teacher.id = u.id) " +
           "WHERE u.id IN :userIds GROUP BY u.id")
    List<Object[]> countLessonsByUserIds(@Param("userIds") List<Long> userIds);

    // Find active lessons for teacher/student by status (for admin deletion with refund)
    List<Lesson> findByTeacherIdAndStatusIn(Long teacherId, Collection<LessonStatus> statuses);
    List<Lesson> findByStudentIdAndStatusIn(Long studentId, Collection<LessonStatus> statuses);

    // Library: completed lessons with recordings for a student
    List<Lesson> findByStudentAndStatusAndRecordingUrlIsNotNullOrderByScheduledAtDesc(User student, LessonStatus status);

    // Library: search and filter completed lessons with recordings
    @Query(value = "SELECT l.* FROM lessons l " +
           "JOIN users t ON t.id = l.teacher_id " +
           "WHERE l.student_id = :studentId " +
           "AND l.status = 'COMPLETED' " +
           "AND l.recording_url IS NOT NULL " +
           "AND (l.deleted_by_student IS NULL OR l.deleted_by_student = false) " +
           "AND (CAST(:search AS VARCHAR) IS NULL OR CAST(:search AS VARCHAR) = '' " +
           "    OR LOWER(t.first_name) LIKE LOWER(CONCAT('%', CAST(:search AS VARCHAR), '%')) " +
           "    OR LOWER(t.last_name) LIKE LOWER(CONCAT('%', CAST(:search AS VARCHAR), '%'))) " +
           "AND (CAST(:dateFrom AS TIMESTAMP) IS NULL OR l.scheduled_at >= CAST(:dateFrom AS TIMESTAMP)) " +
           "AND (CAST(:dateTo AS TIMESTAMP) IS NULL OR l.scheduled_at <= CAST(:dateTo AS TIMESTAMP)) " +
           "ORDER BY l.scheduled_at DESC", nativeQuery = true)
    List<Lesson> findLibraryVideos(
            @Param("studentId") Long studentId,
            @Param("search") String search,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo
    );

    // Admin: Find lessons with messages/notes for review
    @Query("SELECT l FROM Lesson l " +
           "LEFT JOIN FETCH l.student " +
           "LEFT JOIN FETCH l.teacher " +
           "WHERE l.createdAt BETWEEN :startDate AND :endDate " +
           "AND (l.notes IS NOT NULL AND l.notes <> '' " +
           "  OR l.teacherObservations IS NOT NULL AND l.teacherObservations <> '' " +
           "  OR l.teacherComment IS NOT NULL AND l.teacherComment <> '') " +
           "AND (:teacherId IS NULL OR l.teacher.id = :teacherId) " +
           "AND (:studentId IS NULL OR l.student.id = :studentId) " +
           "ORDER BY l.createdAt DESC")
    List<Lesson> findLessonsWithMessages(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("teacherId") Long teacherId,
            @Param("studentId") Long studentId
    );
}
