package com.chessconnect.scheduler;

import com.chessconnect.model.Lesson;
import com.chessconnect.repository.LessonRepository;
import com.chessconnect.service.VideoConcatenationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Scheduler that checks for lessons with multiple recording segments
 * and triggers video concatenation.
 *
 * Runs every 10 minutes to process completed lessons with segments.
 * Stops retrying after MAX_RETRIES failures per lesson.
 */
@Component
public class VideoConcatenationScheduler {

    private static final Logger log = LoggerFactory.getLogger(VideoConcatenationScheduler.class);
    private static final int MAX_RETRIES = 5;

    private final LessonRepository lessonRepository;
    private final VideoConcatenationService concatenationService;
    private final Map<Long, Integer> failureCounts = new ConcurrentHashMap<>();

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
            LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(60);

            List<Lesson> lessonsWithSegments = lessonRepository.findLessonsNeedingConcatenation(cutoffTime);

            if (lessonsWithSegments.isEmpty()) {
                log.debug("No lessons with segments to concatenate");
                return;
            }

            log.info("Found {} lessons with segments to concatenate", lessonsWithSegments.size());

            int successCount = 0;
            int failureCount = 0;
            int skippedCount = 0;

            for (Lesson lesson : lessonsWithSegments) {
                int previousFailures = failureCounts.getOrDefault(lesson.getId(), 0);
                if (previousFailures >= MAX_RETRIES) {
                    skippedCount++;
                    continue;
                }

                try {
                    log.info("Processing concatenation for lesson {} (segments: {}, attempt: {})",
                            lesson.getId(), lesson.getRecordingSegmentsList().size(), previousFailures + 1);

                    boolean success = concatenationService.concatenateRecordingSegments(lesson.getId());

                    if (success) {
                        successCount++;
                        failureCounts.remove(lesson.getId());
                    } else {
                        failureCount++;
                        failureCounts.put(lesson.getId(), previousFailures + 1);
                        if (previousFailures + 1 >= MAX_RETRIES) {
                            log.error("Concatenation permanently failed for lesson {} after {} attempts",
                                    lesson.getId(), MAX_RETRIES);
                        }
                    }

                    // Sleep briefly between concatenations to avoid overloading the system
                    Thread.sleep(2000);

                } catch (Exception e) {
                    failureCount++;
                    failureCounts.put(lesson.getId(), previousFailures + 1);
                    log.error("Error processing concatenation for lesson {}", lesson.getId(), e);
                }
            }

            if (skippedCount > 0) {
                log.warn("Concatenation batch complete: {} successful, {} failed, {} skipped (max retries)",
                        successCount, failureCount, skippedCount);
            } else {
                log.info("Concatenation batch complete: {} successful, {} failed", successCount, failureCount);
            }

        } catch (Exception e) {
            log.error("Error in video concatenation scheduler", e);
        }
    }

    /**
     * Manual trigger for concatenation (useful for testing or manual intervention).
     * Resets failure count for the lesson.
     */
    public void triggerConcatenationForLesson(Long lessonId) {
        log.info("Manual concatenation trigger for lesson {}", lessonId);
        failureCounts.remove(lessonId);
        try {
            concatenationService.concatenateRecordingSegments(lessonId);
        } catch (Exception e) {
            log.error("Error in manual concatenation for lesson {}", lessonId, e);
        }
    }
}
