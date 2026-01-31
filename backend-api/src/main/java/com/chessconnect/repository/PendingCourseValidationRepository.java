package com.chessconnect.repository;

import com.chessconnect.model.PendingCourseValidation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PendingCourseValidationRepository extends JpaRepository<PendingCourseValidation, Long> {

    List<PendingCourseValidation> findByTeacherIdAndDismissedFalseOrderByCreatedAtDesc(Long teacherId);

    Optional<PendingCourseValidation> findByLessonId(Long lessonId);

    void deleteByTeacherIdAndStudentId(Long teacherId, Long studentId);

    void deleteByLessonId(Long lessonId);

    boolean existsByLessonId(Long lessonId);

    int countByTeacherIdAndDismissedFalse(Long teacherId);
}
