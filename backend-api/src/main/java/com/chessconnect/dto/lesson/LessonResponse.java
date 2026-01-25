package com.chessconnect.dto.lesson;

import com.chessconnect.model.Course;
import com.chessconnect.model.Lesson;
import com.chessconnect.model.Progress;
import com.chessconnect.model.User;
import com.chessconnect.model.enums.LessonStatus;

import java.time.LocalDateTime;

public record LessonResponse(
        Long id,
        Long studentId,
        String studentName,
        String studentLevel,
        Integer studentAge,
        Integer studentElo,
        Long teacherId,
        String teacherName,
        LocalDateTime scheduledAt,
        Integer durationMinutes,
        String zoomLink,
        LessonStatus status,
        Integer priceCents,
        Integer commissionCents,
        Integer teacherEarningsCents,
        Boolean isFromSubscription,
        Boolean isFreeTrial,
        String notes,
        String cancellationReason,
        String cancelledBy,
        Integer refundPercentage,
        Integer refundedAmountCents,
        String teacherObservations,
        String recordingUrl,
        LocalDateTime teacherJoinedAt,
        LocalDateTime createdAt,
        Long courseId,
        String courseTitle,
        String courseGrade
) {
    public static LessonResponse from(Lesson lesson) {
        User student = lesson.getStudent();
        Progress progress = student.getProgress();
        String level = progress != null ? progress.getCurrentLevel().name() : null;

        Course course = lesson.getCourse();
        Long courseId = course != null ? course.getId() : null;
        String courseTitle = course != null ? course.getTitle() : null;
        String courseGrade = course != null ? course.getGrade().name() : null;

        return new LessonResponse(
                lesson.getId(),
                student.getId(),
                student.getFullName(),
                level,
                student.getAge(),
                student.getEloRating(),
                lesson.getTeacher().getId(),
                lesson.getTeacher().getDisplayName(),
                lesson.getScheduledAt(),
                lesson.getDurationMinutes(),
                lesson.getZoomLink(),
                lesson.getStatus(),
                lesson.getPriceCents(),
                lesson.getCommissionCents(),
                lesson.getTeacherEarningsCents(),
                lesson.getIsFromSubscription(),
                lesson.getIsFreeTrial(),
                lesson.getNotes(),
                lesson.getCancellationReason(),
                lesson.getCancelledBy(),
                lesson.getRefundPercentage(),
                lesson.getRefundedAmountCents(),
                lesson.getTeacherObservations(),
                lesson.getRecordingUrl(),
                lesson.getTeacherJoinedAt(),
                lesson.getCreatedAt(),
                courseId,
                courseTitle,
                courseGrade
        );
    }
}
