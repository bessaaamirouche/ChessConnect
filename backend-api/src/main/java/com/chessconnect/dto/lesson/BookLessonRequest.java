package com.chessconnect.dto.lesson;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record BookLessonRequest(
        @NotNull(message = "Teacher ID is required")
        Long teacherId,

        @NotNull(message = "Scheduled date/time is required")
        LocalDateTime scheduledAt,

        Integer durationMinutes,

        String notes,

        Boolean useSubscription
) {
    public BookLessonRequest {
        if (durationMinutes == null) {
            durationMinutes = 60;
        }
        if (useSubscription == null) {
            useSubscription = true;
        }
    }
}
