package com.chessconnect.repository;

import com.chessconnect.model.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {

    List<Rating> findByTeacherIdOrderByCreatedAtDesc(Long teacherId);

    Optional<Rating> findByLessonId(Long lessonId);

    boolean existsByLessonId(Long lessonId);

    @Query("SELECT AVG(r.stars) FROM Rating r WHERE r.teacher.id = :teacherId")
    Double getAverageRatingForTeacher(@Param("teacherId") Long teacherId);

    @Query("SELECT COUNT(r) FROM Rating r WHERE r.teacher.id = :teacherId")
    Integer getReviewCountForTeacher(@Param("teacherId") Long teacherId);

    List<Rating> findByStudentIdOrderByCreatedAtDesc(Long studentId);

    void deleteByTeacherId(Long teacherId);

    void deleteByStudentId(Long studentId);
}
