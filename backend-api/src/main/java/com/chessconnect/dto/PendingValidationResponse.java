package com.chessconnect.dto;

import com.chessconnect.model.PendingCourseValidation;
import java.time.LocalDateTime;

public record PendingValidationResponse(
    Long id,
    Long lessonId,
    Long studentId,
    String studentName,
    LocalDateTime createdAt
) {
    public static PendingValidationResponse fromEntity(PendingCourseValidation entity) {
        return new PendingValidationResponse(
            entity.getId(),
            entity.getLesson().getId(),
            entity.getStudent().getId(),
            entity.getStudentName(),
            entity.getCreatedAt()
        );
    }
}
