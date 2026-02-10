package com.chessconnect.scheduler;

import com.chessconnect.model.Lesson;
import com.chessconnect.model.enums.LessonStatus;
import com.chessconnect.repository.LessonRepository;
import com.chessconnect.service.VideoConcatenationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduler that checks for lessons with multiple recording segments
 * and triggers video concatenation.
 *
 * Runs every 10 minutes to process completed lessons with segments.
 */
@Component
public class VideoConcatenationScheduler {

    private static final Logger log = LoggerFactory.getLogger(VideoConcatenationScheduler.class);

    private final LessonRepository lessonRepository;
    private final VideoConcatenationService concatenationService;

    public VideoConcatenationScheduler(LessonRepository lessonRepository,
                                        VideoConcatenationService concatenationService) {
        this.lessonRepository = lessonRepository;
        this.concatenationService = concatenationService;
    }

    /**
     * Check for lessons with multiple segments and trigger concatenation.
     * Only processes lessons that are:
     * - COMPLETED status
     * - Have recording_segments not null
     * - Scheduled at least 60 minutes ago (to ensure all segments are received)
     */
    @Scheduled(fixedRate = 600000) // Every 10 minutes
    public void processPendingConcatenations() {
        if (!concatenationService.isFFmpegAvailable()) {
            log.warn("FFmpeg not available, skipping video concatenation");
            return;
        }

        try {
            // Find lessons that need concatenation
            // - Must have recording_segments
            // - Must be completed
            // - Must be at least 60 minutes old (to ensure all segments received)
            LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(60);

            List<Lesson> lessonsWithSegments = lessonRepository.findAll().stream()
                    .filter(lesson -> lesson.getRecordingSegments() != null && !lesson.getRecordingSegments().isBlank())
                    .filter(lesson -> lesson.getStatus() == LessonStatus.COMPLETED)
                    .filter(lesson -> lesson.getScheduledAt().isBefore(cutoffTime))
                    .toList();

            if (lessonsWithSegments.isEmpty()) {
                log.debug("No lessons with segments to concatenate");
                return;
            }

            log.info("Found {} lessons with segments to concatenate", lessonsWithSegments.size());

            int successCount = 0;
            int failureCount = 0;

            for (Lesson lesson : lessonsWithSegments) {
                try {
                    log.info("Processing concatenation for lesson {} (segments: {})",
                            lesson.getId(), lesson.getRecordingSegmentsList().size());

                    boolean success = concatenationService.concatenateRecordingSegments(lesson.getId());

                    if (success) {
                        successCount++;
                    } else {
                        failureCount++;
                        log.warn("Concatenation failed for lesson {}", lesson.getId());
                    }

                    // Sleep briefly between concatenations to avoid overloading the system
                    Thread.sleep(2000);

                } catch (Exception e) {
                    failureCount++;
                    log.error("Error processing concatenation for lesson {}", lesson.getId(), e);
                }
            }

            log.info("Concatenation batch complete: {} successful, {} failed", successCount, failureCount);

        } catch (Exception e) {
            log.error("Error in video concatenation scheduler", e);
        }
    }

    /**
     * Manual trigger for concatenation (useful for testing or manual intervention).
     * Can be called from an admin endpoint.
     */
    public void triggerConcatenationForLesson(Long lessonId) {
        log.info("Manual concatenation trigger for lesson {}", lessonId);
        try {
            concatenationService.concatenateRecordingSegments(lessonId);
        } catch (Exception e) {
            log.error("Error in manual concatenation for lesson {}", lessonId, e);
        }
    }
}
