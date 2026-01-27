package com.chessconnect.repository;

import com.chessconnect.model.TeacherBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TeacherBalanceRepository extends JpaRepository<TeacherBalance, Long> {
    Optional<TeacherBalance> findByTeacherId(Long teacherId);

    @Modifying
    void deleteByTeacherId(Long teacherId);
}
