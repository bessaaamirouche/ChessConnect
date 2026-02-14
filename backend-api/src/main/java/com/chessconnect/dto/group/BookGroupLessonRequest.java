package com.chessconnect.dto.group;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record BookGroupLessonRequest(
    @NotNull Long teacherId,
    @NotNull LocalDateTime scheduledAt,
    Integer durationMinutes,
    String notes,
    Integer targetGroupSize, // now optional — read from availability if null
    Long courseId
) {}
