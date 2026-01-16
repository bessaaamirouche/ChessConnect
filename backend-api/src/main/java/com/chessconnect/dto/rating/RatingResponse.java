package com.chessconnect.dto.rating;

import com.chessconnect.model.Rating;

import java.time.LocalDateTime;

public record RatingResponse(
    Long id,
    Long lessonId,
    Long studentId,
    String studentFirstName,
    String studentLastName,
    Long teacherId,
    String teacherFirstName,
    String teacherLastName,
    Integer stars,
    String comment,
    LocalDateTime createdAt
) {
    public static RatingResponse from(Rating rating) {
        return new RatingResponse(
            rating.getId(),
            rating.getLesson().getId(),
            rating.getStudent().getId(),
            rating.getStudent().getFirstName(),
            rating.getStudent().getLastName(),
            rating.getTeacher().getId(),
            rating.getTeacher().getFirstName(),
            rating.getTeacher().getLastName(),
            rating.getStars(),
            rating.getComment(),
            rating.getCreatedAt()
        );
    }
}
