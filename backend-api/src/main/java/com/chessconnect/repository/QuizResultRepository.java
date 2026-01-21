package com.chessconnect.repository;

import com.chessconnect.model.QuizResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface QuizResultRepository extends JpaRepository<QuizResult, Long> {

    Optional<QuizResult> findTopByStudentIdOrderByCompletedAtDesc(Long studentId);

    List<QuizResult> findByStudentIdOrderByCompletedAtDesc(Long studentId);

    void deleteByStudentId(Long studentId);
}
