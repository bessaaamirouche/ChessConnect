package com.chessconnect.dto.programme;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateCurrentCourseRequest(
    @NotNull(message = "Course ID is required")
    @Min(value = 1, message = "Course ID must be positive")
    Integer courseId
) {}
