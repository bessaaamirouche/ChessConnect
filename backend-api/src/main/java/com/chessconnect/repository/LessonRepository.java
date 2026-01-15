package com.chessconnect.repository;

import com.chessconnect.model.Lesson;
import com.chessconnect.model.enums.LessonStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

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

    // For migration: find completed lessons that haven't been credited yet
    @Query("SELECT l FROM Lesson l WHERE l.status = 'COMPLETED' AND (l.earningsCredited IS NULL OR l.earningsCredited = false)")
    List<Lesson> findCompletedLessonsNotCredited();
}
