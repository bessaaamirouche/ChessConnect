package com.chessconnect.event.payload;

/**
 * Payload for teacher joined video call events.
 */
public record TeacherJoinedPayload(
    Long lessonId,
    String teacherName
) {}
