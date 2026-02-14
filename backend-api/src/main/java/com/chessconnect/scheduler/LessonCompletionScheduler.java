package com.chessconnect.scheduler;

import com.chessconnect.model.Lesson;
import com.chessconnect.model.enums.LessonStatus;
import com.chessconnect.repository.LessonRepository;
import com.chessconnect.service.TeacherBalanceService;
import com.chessconnect.repository.ProgressRepository;
import com.chessconnect.model.Progress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduler that automatically completes lessons after their full duration has elapsed.
 * Uses each lesson's durationMinutes field (default 60) to determine when to auto-complete.
 */
@Component
public class LessonCompletionScheduler {

    private static final Logger log = LoggerFactory.getLogger(LessonCompletionScheduler.class);

    private final LessonRepository lessonRepository;
    private final TeacherBalanceService teacherBalanceService;
    private final ProgressRepository progressRepository;

    public LessonCompletionScheduler(
            LessonRepository lessonRepository,
            TeacherBalanceService teacherBalanceService,
            ProgressRepository progressRepository
    ) {
        this.lessonRepository = lessonRepository;
        this.teacherBalanceService = teacherBalanceService;
        this.progressRepository = progressRepository;
    }

    /**
     * Runs every 5 minutes to check for lessons that should be auto-completed.
     * A lesson is auto-completed if:
     * - Status is CONFIRMED
     * - scheduledAt + durationMinutes has passed (full lesson duration elapsed)
     */
    @Scheduled(fixedRate = 300000) // Every 5 minutes (300000 ms)
    @Transactional
    public void autoCompleteLessons() {
        LocalDateTime now = LocalDateTime.now();

        List<Lesson> confirmedLessons = lessonRepository.findByStatusOrderByScheduledAtDesc(LessonStatus.CONFIRMED);

        List<Lesson> lessonsToComplete = confirmedLessons.stream()
                .filter(lesson -> {
                    LocalDateTime lessonEnd = lesson.getScheduledAt()
                            .plusMinutes(lesson.getDurationMinutes());
                    return now.isAfter(lessonEnd);
                })
                .toList();

        if (lessonsToComplete.isEmpty()) {
            return;
        }

        log.info("Auto-completing {} lessons whose full duration has elapsed", lessonsToComplete.size());

        for (Lesson lesson : lessonsToComplete) {
            try {
                completeLesson(lesson);
            } catch (Exception e) {
                log.error("Failed to auto-complete lesson {}: {}", lesson.getId(), e.getMessage());
            }
        }
    }

    private void completeLesson(Lesson lesson) {
        lesson.setStatus(LessonStatus.COMPLETED);

        // Credit teacher earnings
        if (!Boolean.TRUE.equals(lesson.getEarningsCredited())) {
            teacherBalanceService.creditEarningsForCompletedLesson(lesson);
            lesson.setEarningsCredited(true);
        }

        // Update student progress
        Progress progress = progressRepository.findByStudentId(lesson.getStudent().getId())
                .orElse(null);
        if (progress != null) {
            progress.recordCompletedLesson();
            progressRepository.save(progress);
        }

        lessonRepository.save(lesson);

        log.info("Auto-completed lesson {} (teacher: {}, student: {}, scheduled: {})",
                lesson.getId(),
                lesson.getTeacher().getEmail(),
                lesson.getStudent().getEmail(),
                lesson.getScheduledAt());
    }
}
