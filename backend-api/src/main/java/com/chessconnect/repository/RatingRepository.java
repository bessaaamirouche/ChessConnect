package com.chessconnect.repository;

import com.chessconnect.model.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Modifying;

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

    @Modifying
    void deleteByTeacherId(Long teacherId);

    @Modifying
    void deleteByStudentId(Long studentId);

    @Modifying
    @Query(value = "DELETE FROM ratings WHERE lesson_id IN (SELECT id FROM lessons WHERE student_id = :userId OR teacher_id = :userId)", nativeQuery = true)
    void deleteByLessonUserId(@Param("userId") Long userId);

    // Batch queries for ratings - fixes N+1 in AdminService.getUsers()
    @Query("SELECT r.teacher.id, AVG(r.stars), COUNT(r) FROM Rating r WHERE r.teacher.id IN :teacherIds GROUP BY r.teacher.id")
    List<Object[]> getRatingsStatsByTeacherIds(@Param("teacherIds") List<Long> teacherIds);
}
