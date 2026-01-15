package com.chessconnect.dto.learningpath;

import com.chessconnect.model.Course;
import com.chessconnect.model.UserCourseProgress;
import com.chessconnect.model.enums.ChessLevel;
import com.chessconnect.model.enums.CourseStatus;

import java.time.LocalDateTime;

public record CourseResponse(
    Long id,
    String title,
    String description,
    String content,
    ChessLevel grade,
    Integer orderInGrade,
    Integer estimatedMinutes,
    String iconName,
    CourseStatus status,
    LocalDateTime startedAt,
    LocalDateTime completedAt,
    Long validatedByTeacherId,
    String validatedByTeacherName,
    LocalDateTime validatedAt
) {
    public static CourseResponse fromEntity(Course course, UserCourseProgress progress) {
        return fromEntity(course, progress, null);
    }

    public static CourseResponse fromEntity(Course course, UserCourseProgress progress, String teacherName) {
        return new CourseResponse(
            course.getId(),
            course.getTitle(),
            course.getDescription(),
            course.getContent(),
            course.getGrade(),
            course.getOrderInGrade(),
            course.getEstimatedMinutes(),
            course.getIconName(),
            progress != null ? progress.getStatus() : CourseStatus.LOCKED,
            progress != null ? progress.getStartedAt() : null,
            progress != null ? progress.getCompletedAt() : null,
            progress != null ? progress.getValidatedByTeacherId() : null,
            teacherName,
            progress != null ? progress.getValidatedAt() : null
        );
    }

    public static CourseResponse fromEntityWithoutContent(Course course, UserCourseProgress progress) {
        return fromEntityWithoutContent(course, progress, null);
    }

    public static CourseResponse fromEntityWithoutContent(Course course, UserCourseProgress progress, String teacherName) {
        return new CourseResponse(
            course.getId(),
            course.getTitle(),
            course.getDescription(),
            null,
            course.getGrade(),
            course.getOrderInGrade(),
            course.getEstimatedMinutes(),
            course.getIconName(),
            progress != null ? progress.getStatus() : CourseStatus.LOCKED,
            progress != null ? progress.getStartedAt() : null,
            progress != null ? progress.getCompletedAt() : null,
            progress != null ? progress.getValidatedByTeacherId() : null,
            teacherName,
            progress != null ? progress.getValidatedAt() : null
        );
    }
}
