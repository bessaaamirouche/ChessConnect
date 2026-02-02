package com.chessconnect.event.payload;

/**
 * Payload for lesson status change events.
 */
public record LessonStatusPayload(
    Long lessonId,
    String oldStatus,
    String newStatus,
    String teacherName,
    String studentName,
    String scheduledAt
) {}
