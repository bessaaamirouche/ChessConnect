package com.chessconnect.repository;

import com.chessconnect.model.UserCourseProgress;
import com.chessconnect.model.enums.ChessLevel;
import com.chessconnect.model.enums.CourseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserCourseProgressRepository extends JpaRepository<UserCourseProgress, Long> {

    List<UserCourseProgress> findByUserId(Long userId);

    Optional<UserCourseProgress> findByUserIdAndCourseId(Long userId, Long courseId);

    @Query("SELECT ucp FROM UserCourseProgress ucp WHERE ucp.user.id = :userId AND ucp.course.grade = :grade ORDER BY ucp.course.orderInGrade")
    List<UserCourseProgress> findByUserIdAndGrade(@Param("userId") Long userId, @Param("grade") ChessLevel grade);

    @Query("SELECT COUNT(ucp) FROM UserCourseProgress ucp WHERE ucp.user.id = :userId AND ucp.course.grade = :grade AND ucp.status = :status")
    long countByUserIdAndGradeAndStatus(@Param("userId") Long userId, @Param("grade") ChessLevel grade, @Param("status") CourseStatus status);

    @Query("SELECT COUNT(ucp) FROM UserCourseProgress ucp WHERE ucp.user.id = :userId AND ucp.status = :status")
    long countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") CourseStatus status);

    boolean existsByUserIdAndCourseId(Long userId, Long courseId);

    @Modifying
    void deleteByUserId(Long userId);
}
