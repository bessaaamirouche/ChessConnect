package com.chessconnect.repository;

import com.chessconnect.model.QuizQuestion;
import com.chessconnect.model.enums.ChessLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {

    List<QuizQuestion> findByLevelOrderByOrderInLevel(ChessLevel level);

    List<QuizQuestion> findAllByOrderByLevelAscOrderInLevelAsc();

    long countByLevel(ChessLevel level);
}
