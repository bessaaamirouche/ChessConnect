package com.chessconnect.dto.exercise;

import com.chessconnect.model.Exercise;
import com.chessconnect.model.enums.ChessLevel;
import com.chessconnect.model.enums.DifficultyLevel;

public record ExerciseResponse(
    Long id,
    Long lessonId,
    String title,
    String description,
    String startingFen,
    DifficultyLevel difficultyLevel,
    int stockfishSkillLevel,
    int thinkTimeMs,
    ChessLevel chessLevel,
    String playerColor,
    Integer timeLimitSeconds
) {
    public static ExerciseResponse from(Exercise exercise) {
        return new ExerciseResponse(
            exercise.getId(),
            exercise.getLesson() != null ? exercise.getLesson().getId() : null,
            exercise.getTitle(),
            exercise.getDescription(),
            exercise.getStartingFen(),
            exercise.getDifficultyLevel(),
            exercise.getDifficultyLevel().getStockfishSkillLevel(),
            exercise.getDifficultyLevel().getThinkTimeMs(),
            exercise.getChessLevel(),
            exercise.getPlayerColor(),
            exercise.getTimeLimitSeconds()
        );
    }
}
