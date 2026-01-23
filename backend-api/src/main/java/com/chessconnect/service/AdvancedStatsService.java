package com.chessconnect.service;

import com.chessconnect.dto.stats.AdvancedStatsResponse;
import com.chessconnect.model.Lesson;
import com.chessconnect.model.Progress;
import com.chessconnect.model.enums.LessonStatus;
import com.chessconnect.repository.LessonRepository;
import com.chessconnect.repository.ProgressRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdvancedStatsService {

    private final LessonRepository lessonRepository;
    private final ProgressRepository progressRepository;
    private final SubscriptionService subscriptionService;

    public AdvancedStatsService(
            LessonRepository lessonRepository,
            ProgressRepository progressRepository,
            SubscriptionService subscriptionService
    ) {
        this.lessonRepository = lessonRepository;
        this.progressRepository = progressRepository;
        this.subscriptionService = subscriptionService;
    }

    /**
     * Get advanced statistics for a student. Premium only.
     */
    public AdvancedStatsResponse getAdvancedStats(Long studentId) {
        if (!subscriptionService.isPremium(studentId)) {
            throw new IllegalArgumentException("Cette fonctionnalité est réservée aux abonnés Premium");
        }

        List<Lesson> allLessons = lessonRepository.findByStudentIdOrderByScheduledAtDesc(studentId);
        List<Lesson> completedLessons = allLessons.stream()
                .filter(l -> l.getStatus() == LessonStatus.COMPLETED)
                .toList();

        Progress progress = progressRepository.findByStudentId(studentId).orElse(null);

        // Calculate total hours of learning
        int totalMinutes = completedLessons.stream()
                .mapToInt(Lesson::getDurationMinutes)
                .sum();
        double totalHours = totalMinutes / 60.0;

        // Calculate lessons per month (last 6 months)
        Map<String, Integer> lessonsPerMonth = calculateLessonsPerMonth(completedLessons);

        // Calculate average lessons per week
        double avgLessonsPerWeek = calculateAverageLessonsPerWeek(completedLessons);

        // Get favorite coaches (most lessons with)
        List<AdvancedStatsResponse.CoachStats> favoriteCoaches = calculateFavoriteCoaches(completedLessons);

        // Calculate learning streak (consecutive weeks with at least one lesson)
        int currentStreak = calculateLearningStreak(completedLessons);

        // Get progress level info
        String currentLevel = progress != null ? progress.getCurrentLevel().name() : "PION";
        int completedCourses = progress != null ? progress.getTotalLessonsCompleted() : 0;

        return AdvancedStatsResponse.builder()
                .totalLessons(completedLessons.size())
                .totalHoursLearned(Math.round(totalHours * 10) / 10.0)
                .avgLessonsPerWeek(Math.round(avgLessonsPerWeek * 10) / 10.0)
                .lessonsPerMonth(lessonsPerMonth)
                .favoriteCoaches(favoriteCoaches)
                .currentStreak(currentStreak)
                .currentLevel(currentLevel)
                .completedCourses(completedCourses)
                .build();
    }

    private Map<String, Integer> calculateLessonsPerMonth(List<Lesson> lessons) {
        LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);

        return lessons.stream()
                .filter(l -> l.getScheduledAt().isAfter(sixMonthsAgo))
                .collect(Collectors.groupingBy(
                        l -> l.getScheduledAt().getYear() + "-" + String.format("%02d", l.getScheduledAt().getMonthValue()),
                        TreeMap::new,
                        Collectors.summingInt(l -> 1)
                ));
    }

    private double calculateAverageLessonsPerWeek(List<Lesson> lessons) {
        if (lessons.isEmpty()) return 0;

        // Find earliest and latest lesson dates
        Optional<LocalDateTime> earliest = lessons.stream()
                .map(Lesson::getScheduledAt)
                .min(LocalDateTime::compareTo);

        Optional<LocalDateTime> latest = lessons.stream()
                .map(Lesson::getScheduledAt)
                .max(LocalDateTime::compareTo);

        if (earliest.isEmpty() || latest.isEmpty()) return 0;

        long weeks = ChronoUnit.WEEKS.between(earliest.get(), latest.get());
        if (weeks <= 0) weeks = 1;

        return (double) lessons.size() / weeks;
    }

    private List<AdvancedStatsResponse.CoachStats> calculateFavoriteCoaches(List<Lesson> lessons) {
        Map<Long, List<Lesson>> lessonsByCoach = lessons.stream()
                .collect(Collectors.groupingBy(l -> l.getTeacher().getId()));

        return lessonsByCoach.entrySet().stream()
                .map(entry -> {
                    Lesson sample = entry.getValue().get(0);
                    return AdvancedStatsResponse.CoachStats.builder()
                            .coachId(entry.getKey())
                            .coachName(sample.getTeacher().getFullName())
                            .lessonCount(entry.getValue().size())
                            .totalHours(entry.getValue().stream().mapToInt(Lesson::getDurationMinutes).sum() / 60.0)
                            .build();
                })
                .sorted((a, b) -> Integer.compare(b.getLessonCount(), a.getLessonCount()))
                .limit(5)
                .toList();
    }

    private int calculateLearningStreak(List<Lesson> lessons) {
        if (lessons.isEmpty()) return 0;

        // Group lessons by week
        Set<String> weeksWithLessons = lessons.stream()
                .map(l -> {
                    LocalDateTime date = l.getScheduledAt();
                    int weekOfYear = date.getDayOfYear() / 7;
                    return date.getYear() + "-W" + weekOfYear;
                })
                .collect(Collectors.toSet());

        // Count consecutive weeks from current week going back
        int streak = 0;
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < 52; i++) {
            LocalDateTime weekDate = now.minusWeeks(i);
            int weekOfYear = weekDate.getDayOfYear() / 7;
            String weekKey = weekDate.getYear() + "-W" + weekOfYear;

            if (weeksWithLessons.contains(weekKey)) {
                streak++;
            } else if (i > 0) {
                // Allow one week gap for the current week
                break;
            }
        }

        return streak;
    }
}
