package com.chessconnect.dto.payment;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateLessonCheckoutRequest {
    @NotNull(message = "Teacher ID is required")
    private Long teacherId;

    @NotNull(message = "Scheduled time is required")
    @Future(message = "Scheduled time must be in the future")
    private LocalDateTime scheduledAt;

    private Integer durationMinutes = 60;

    private String notes;

    private boolean embedded = false;
}
