package com.chessconnect.dto.stats;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class AdvancedStatsResponse {
    private int totalLessons;
    private double totalHoursLearned;
    private double avgLessonsPerWeek;
    private Map<String, Integer> lessonsPerMonth;
    private List<CoachStats> favoriteCoaches;
    private int currentStreak;
    private String currentLevel;
    private int completedCourses;

    @Data
    @Builder
    public static class CoachStats {
        private Long coachId;
        private String coachName;
        private int lessonCount;
        private double totalHours;
    }
}
