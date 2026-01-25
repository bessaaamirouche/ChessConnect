package com.chessconnect.dto.exercise;

import com.chessconnect.model.Exercise;
import com.chessconnect.model.Lesson;
import com.chessconnect.model.enums.ChessLevel;
import com.chessconnect.model.enums.DifficultyLevel;
import java.time.LocalDateTime;

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
    Integer timeLimitSeconds,
    String teacherName,
    LocalDateTime lessonDate
) {
    public static ExerciseResponse from(Exercise exercise) {
        Lesson lesson = exercise.getLesson();
        String teacherName = null;
        LocalDateTime lessonDate = null;

        if (lesson != null) {
            teacherName = lesson.getTeacher().getFirstName() + " " + lesson.getTeacher().getLastName();
            lessonDate = lesson.getScheduledAt();
        }

        return new ExerciseResponse(
            exercise.getId(),
            lesson != null ? lesson.getId() : null,
            exercise.getTitle(),
            exercise.getDescription(),
            exercise.getStartingFen(),
            exercise.getDifficultyLevel(),
            exercise.getDifficultyLevel().getStockfishSkillLevel(),
            exercise.getDifficultyLevel().getThinkTimeMs(),
            exercise.getChessLevel(),
            exercise.getPlayerColor(),
            exercise.getTimeLimitSeconds(),
            teacherName,
            lessonDate
        );
    }
}
