package com.chessconnect.dto.wallet;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record BookWithCreditRequest(
        @NotNull(message = "Teacher ID is required")
        Long teacherId,

        @NotNull(message = "Scheduled time is required")
        LocalDateTime scheduledAt,

        @NotNull(message = "Duration is required")
        Integer durationMinutes,

        String notes
) {}
