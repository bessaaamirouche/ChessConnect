package com.chessconnect.dto.group;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record BookGroupLessonRequest(
    @NotNull Long teacherId,
    @NotNull LocalDateTime scheduledAt,
    Integer durationMinutes,
    String notes,
    @NotNull Integer targetGroupSize, // 2 or 3
    Long courseId
) {}
