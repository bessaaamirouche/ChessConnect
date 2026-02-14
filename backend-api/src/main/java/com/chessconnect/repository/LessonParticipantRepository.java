package com.chessconnect.repository;

import com.chessconnect.model.Lesson;
import com.chessconnect.model.LessonParticipant;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LessonParticipantRepository extends JpaRepository<LessonParticipant, Long> {

    List<LessonParticipant> findByLessonId(Long lessonId);

    List<LessonParticipant> findByLessonIdAndStatus(Long lessonId, String status);

    Optional<LessonParticipant> findByLessonIdAndStudentId(Long lessonId, Long studentId);

    int countByLessonIdAndStatus(Long lessonId, String status);

    boolean existsByLessonIdAndStudentId(Long lessonId, Long studentId);

    @Query("SELECT lp FROM LessonParticipant lp WHERE lp.lesson.id = :lessonId AND lp.student.id = :studentId AND lp.status = 'ACTIVE'")
    Optional<LessonParticipant> findActiveByLessonIdAndStudentId(@Param("lessonId") Long lessonId, @Param("studentId") Long studentId);

    @Query("SELECT lp.lesson FROM LessonParticipant lp WHERE lp.student.id = :studentId AND lp.status = 'ACTIVE' AND lp.lesson.status IN ('PENDING', 'CONFIRMED')")
    List<Lesson> findUpcomingGroupLessonsForStudent(@Param("studentId") Long studentId);

    @Query("SELECT lp.lesson FROM LessonParticipant lp WHERE lp.student.id = :studentId AND lp.status = 'ACTIVE' AND lp.lesson.status IN ('COMPLETED', 'CANCELLED')")
    List<Lesson> findHistoryGroupLessonsForStudent(@Param("studentId") Long studentId);

    @Query("SELECT CASE WHEN COUNT(lp) > 0 THEN true ELSE false END FROM LessonParticipant lp WHERE lp.lesson.id = :lessonId AND lp.student.id = :studentId AND lp.status = 'ACTIVE'")
    boolean existsActiveByLessonIdAndStudentId(@Param("lessonId") Long lessonId, @Param("studentId") Long studentId);

    void deleteByStudentId(Long studentId);

    void deleteByLessonId(Long lessonId);
}
