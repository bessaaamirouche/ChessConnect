package com.chessconnect.repository;

import com.chessconnect.model.FavoriteTeacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteTeacherRepository extends JpaRepository<FavoriteTeacher, Long> {

    List<FavoriteTeacher> findByStudentIdOrderByCreatedAtDesc(Long studentId);

    Optional<FavoriteTeacher> findByStudentIdAndTeacherId(Long studentId, Long teacherId);

    boolean existsByStudentIdAndTeacherId(Long studentId, Long teacherId);

    @Modifying
    void deleteByStudentIdAndTeacherId(Long studentId, Long teacherId);

    // Find all students subscribed to notifications for a specific teacher
    List<FavoriteTeacher> findByTeacherIdAndNotifyNewSlotsTrue(Long teacherId);

    // Count favorites for a teacher
    long countByTeacherId(Long teacherId);

    @Modifying
    void deleteByStudentId(Long studentId);

    @Modifying
    void deleteByTeacherId(Long teacherId);
}
