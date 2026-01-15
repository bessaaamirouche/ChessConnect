package com.chessconnect.dto.lesson;

import com.chessconnect.model.enums.LessonStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateLessonStatusRequest(
        @NotNull(message = "Status is required")
        LessonStatus status,

        String cancellationReason,

        String teacherObservations
) {}
