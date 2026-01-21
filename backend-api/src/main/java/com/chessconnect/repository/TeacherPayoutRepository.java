package com.chessconnect.repository;

import com.chessconnect.model.TeacherPayout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeacherPayoutRepository extends JpaRepository<TeacherPayout, Long> {

    List<TeacherPayout> findByTeacherId(Long teacherId);

    Optional<TeacherPayout> findByTeacherIdAndYearMonth(Long teacherId, String yearMonth);

    List<TeacherPayout> findByYearMonth(String yearMonth);

    List<TeacherPayout> findByIsPaidFalse();

    void deleteByTeacherId(Long teacherId);
}
