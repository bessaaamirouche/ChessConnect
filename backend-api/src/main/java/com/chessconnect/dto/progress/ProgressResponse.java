package com.chessconnect.dto.progress;

import com.chessconnect.model.Progress;
import com.chessconnect.model.enums.ChessLevel;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ProgressResponse {
    private Long id;
    private Long studentId;
    private String studentName;
    private ChessLevel currentLevel;
    private String levelDisplayName;
    private String levelDescription;
    private Integer totalLessonsCompleted;
    private Integer lessonsAtCurrentLevel;
    private Integer lessonsRequiredForNextLevel;
    private Double progressPercentage;
    private ChessLevel nextLevel;
    private String nextLevelDisplayName;
    private LocalDateTime lastLessonDate;
    private LocalDateTime createdAt;

    public static ProgressResponse fromEntity(Progress progress) {
        ChessLevel current = progress.getCurrentLevel();
        ChessLevel next = current.nextLevel();

        return ProgressResponse.builder()
                .id(progress.getId())
                .studentId(progress.getStudent().getId())
                .studentName(progress.getStudent().getFullName())
                .currentLevel(current)
                .levelDisplayName(current.getDisplayName())
                .levelDescription(current.getDescription())
                .totalLessonsCompleted(progress.getTotalLessonsCompleted())
                .lessonsAtCurrentLevel(progress.getLessonsAtCurrentLevel())
                .lessonsRequiredForNextLevel(progress.getLessonsRequiredForNextLevel())
                .progressPercentage(progress.getProgressPercentage())
                .nextLevel(current != ChessLevel.DAME ? next : null)
                .nextLevelDisplayName(current != ChessLevel.DAME ? next.getDisplayName() : null)
                .lastLessonDate(progress.getLastLessonDate())
                .createdAt(progress.getCreatedAt())
                .build();
    }
}
