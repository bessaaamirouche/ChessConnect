package com.chessconnect.repository;

import com.chessconnect.model.Exercise;
import com.chessconnect.model.enums.ChessLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExerciseRepository extends JpaRepository<Exercise, Long> {

    Optional<Exercise> findByLessonId(Long lessonId);

    List<Exercise> findByChessLevelOrderByCreatedAtDesc(ChessLevel chessLevel);

    List<Exercise> findAllByOrderByCreatedAtDesc();

    boolean existsByLessonId(Long lessonId);
}
