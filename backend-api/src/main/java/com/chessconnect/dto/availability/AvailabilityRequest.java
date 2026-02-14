package com.chessconnect.dto.availability;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class AvailabilityRequest {

    private DayOfWeek dayOfWeek;

    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    private LocalTime endTime;

    private Boolean isRecurring = true;

    private LocalDate specificDate;

    private String lessonType = "INDIVIDUAL";
}
