package com.chessconnect.dto.availability;

import com.chessconnect.model.Availability;
import lombok.Builder;
import lombok.Data;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
public class AvailabilityResponse {
    private Long id;
    private Long teacherId;
    private String teacherName;
    private DayOfWeek dayOfWeek;
    private String dayOfWeekLabel;
    private LocalTime startTime;
    private LocalTime endTime;
    private Boolean isRecurring;
    private LocalDate specificDate;
    private Boolean isActive;
    private Integer durationMinutes;

    public static AvailabilityResponse fromEntity(Availability availability) {
        return AvailabilityResponse.builder()
                .id(availability.getId())
                .teacherId(availability.getTeacher().getId())
                .teacherName(availability.getTeacher().getFullName())
                .dayOfWeek(availability.getDayOfWeek())
                .dayOfWeekLabel(getDayLabel(availability.getDayOfWeek()))
                .startTime(availability.getStartTime())
                .endTime(availability.getEndTime())
                .isRecurring(availability.getIsRecurring())
                .specificDate(availability.getSpecificDate())
                .isActive(availability.getIsActive())
                .durationMinutes(availability.getDurationMinutes())
                .build();
    }

    private static String getDayLabel(DayOfWeek day) {
        if (day == null) return null;
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
