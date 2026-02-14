package com.chessconnect.dto.availability;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
public class TimeSlotResponse {
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private LocalDateTime dateTime;
    private Boolean isAvailable;
    private String dayOfWeekLabel;
    private String lessonType;

    public static TimeSlotResponse create(LocalDate date, LocalTime startTime, LocalTime endTime, boolean isAvailable, String lessonType) {
        return TimeSlotResponse.builder()
                .date(date)
                .startTime(startTime)
                .endTime(endTime)
                .dateTime(LocalDateTime.of(date, startTime))
                .isAvailable(isAvailable)
                .dayOfWeekLabel(getDayLabel(date.getDayOfWeek()))
                .lessonType(lessonType)
                .build();
    }

    private static String getDayLabel(java.time.DayOfWeek day) {
        return switch (day) {
            case MONDAY -> "Lundi";
            case TUESDAY -> "Mardi";
            case WEDNESDAY -> "Mercredi";
            case THURSDAY -> "Jeudi";
            case FRIDAY -> "Vendredi";
            case SATURDAY -> "Samedi";
            case SUNDAY -> "Dimanche";
        };
    }
}
