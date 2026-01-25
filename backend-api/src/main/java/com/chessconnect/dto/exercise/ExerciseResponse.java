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
    LocalDateTime lessonDate,
    String courseTitle
) {
    public static ExerciseResponse from(Exercise exercise) {
        Lesson lesson = exercise.getLesson();
        String teacherName = null;
        LocalDateTime lessonDate = null;

        if (lesson != null) {
            // Only use first name for privacy
            teacherName = lesson.getTeacher().getFirstName();
            lessonDate = lesson.getScheduledAt();
        }

        // Extract course title - if it contains "avec" or "Entrainement", it's old format
        String courseTitle = exercise.getTitle();
        if (courseTitle != null) {
            // Clean old formats: "Entrainement - Name" or "Cours du XX/XX avec Name"
            if (courseTitle.contains(" - ") && courseTitle.toLowerCase().contains("entrainement")) {
                courseTitle = null; // Will use fallback in frontend
            } else if (courseTitle.toLowerCase().contains(" avec ")) {
                courseTitle = null; // Will use fallback in frontend
            }
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
            lessonDate,
            courseTitle
        );
    }
}
