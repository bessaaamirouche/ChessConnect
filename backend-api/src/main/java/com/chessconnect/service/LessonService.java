package com.chessconnect.service;

import com.chessconnect.dto.lesson.BookLessonRequest;
import com.chessconnect.dto.lesson.LessonResponse;
import com.chessconnect.dto.lesson.UpdateLessonStatusRequest;
import com.chessconnect.dto.zoom.ZoomMeetingResponse;
import com.chessconnect.model.Lesson;
import com.chessconnect.model.Payment;
import com.chessconnect.model.Progress;
import com.chessconnect.model.Subscription;
import com.chessconnect.model.User;
import com.chessconnect.model.enums.LessonStatus;
import com.chessconnect.model.enums.PaymentStatus;
import com.chessconnect.model.enums.UserRole;
import com.chessconnect.repository.LessonRepository;
import com.chessconnect.repository.PaymentRepository;
import com.chessconnect.repository.ProgressRepository;
import com.chessconnect.repository.SubscriptionRepository;
import com.chessconnect.repository.UserRepository;
import com.chessconnect.service.zoom.ZoomService;
import com.stripe.model.Refund;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class LessonService {

    private static final Logger log = LoggerFactory.getLogger(LessonService.class);

    // Cancellation policy constants
    private static final int FULL_REFUND_HOURS = 24;       // > 24h before = 100% refund
    private static final int PARTIAL_REFUND_HOURS = 2;     // 2-24h before = 50% refund
    private static final int TEACHER_CONFIRMATION_HOURS = 24; // Teacher must confirm within 24h

    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ProgressRepository progressRepository;
    private final PaymentRepository paymentRepository;
    private final ZoomService zoomService;
    private final TeacherBalanceService teacherBalanceService;
    private final StripeService stripeService;
    private final GoogleCalendarService googleCalendarService;

    public LessonService(
            LessonRepository lessonRepository,
            UserRepository userRepository,
            SubscriptionRepository subscriptionRepository,
            ProgressRepository progressRepository,
            PaymentRepository paymentRepository,
            ZoomService zoomService,
            TeacherBalanceService teacherBalanceService,
            StripeService stripeService,
            GoogleCalendarService googleCalendarService
    ) {
        this.lessonRepository = lessonRepository;
        this.userRepository = userRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.progressRepository = progressRepository;
        this.paymentRepository = paymentRepository;
        this.zoomService = zoomService;
        this.teacherBalanceService = teacherBalanceService;
        this.stripeService = stripeService;
        this.googleCalendarService = googleCalendarService;
    }

    @Transactional
    public LessonResponse bookLesson(Long studentId, BookLessonRequest request) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));

        if (student.getRole() != UserRole.STUDENT) {
            throw new IllegalArgumentException("Only students can book lessons");
        }

        User teacher = userRepository.findById(request.teacherId())
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found"));

        if (teacher.getRole() != UserRole.TEACHER) {
            throw new IllegalArgumentException("Selected user is not a teacher");
        }

        // Validate that the lesson end time is still in the future
        LocalDateTime lessonEnd = request.scheduledAt().plusMinutes(request.durationMinutes());
        if (!lessonEnd.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Lesson must end in the future");
        }

        checkTeacherAvailability(teacher.getId(), request.scheduledAt(), request.durationMinutes());

        // Check for student time conflicts
        checkStudentTimeConflict(studentId, request.scheduledAt(), request.durationMinutes());

        Lesson lesson = new Lesson();
        lesson.setStudent(student);
        lesson.setTeacher(teacher);
        lesson.setScheduledAt(request.scheduledAt());
        lesson.setDurationMinutes(request.durationMinutes());
        lesson.setNotes(request.notes());
        lesson.setStatus(LessonStatus.PENDING);

        Subscription activeSubscription = null;
        if (request.useSubscription() && teacher.getAcceptsSubscription()) {
            activeSubscription = subscriptionRepository
                    .findByStudentIdAndIsActiveTrue(studentId)
                    .orElse(null);
        }

        if (activeSubscription != null && activeSubscription.hasRemainingLessons()) {
            lesson.setIsFromSubscription(true);
            lesson.setSubscription(activeSubscription);
            int lessonPrice = activeSubscription.getPriceCents() / activeSubscription.getMonthlyQuota();
            lesson.setPriceCents(lessonPrice);
            activeSubscription.setLessonsUsedThisMonth(activeSubscription.getLessonsUsedThisMonth() + 1);
        } else {
            lesson.setIsFromSubscription(false);
            lesson.setPriceCents(teacher.getHourlyRateCents());
        }

        Lesson savedLesson = lessonRepository.save(lesson);
        return LessonResponse.from(savedLesson);
    }

    /**
     * Book a free trial lesson for first-time students.
     * Each student is eligible for one free trial lesson.
     */
    @Transactional
    public LessonResponse bookFreeTrialLesson(Long studentId, BookLessonRequest request) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));

        if (student.getRole() != UserRole.STUDENT) {
            throw new IllegalArgumentException("Only students can book lessons");
        }

        // Check if student has already used their free trial
        if (Boolean.TRUE.equals(student.getHasUsedFreeTrial())) {
            throw new IllegalArgumentException("Vous avez déjà utilisé votre cours d'essai gratuit");
        }

        User teacher = userRepository.findById(request.teacherId())
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found"));

        if (teacher.getRole() != UserRole.TEACHER) {
            throw new IllegalArgumentException("Selected user is not a teacher");
        }

        // Validate that the lesson end time is still in the future
        LocalDateTime lessonEnd = request.scheduledAt().plusMinutes(request.durationMinutes());
        if (!lessonEnd.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Lesson must end in the future");
        }

        checkTeacherAvailability(teacher.getId(), request.scheduledAt(), request.durationMinutes());
        checkStudentTimeConflict(studentId, request.scheduledAt(), request.durationMinutes());

        // Create the free trial lesson
        Lesson lesson = new Lesson();
        lesson.setStudent(student);
        lesson.setTeacher(teacher);
        lesson.setScheduledAt(request.scheduledAt());
        lesson.setDurationMinutes(request.durationMinutes());
        lesson.setNotes(request.notes() != null ? request.notes() + " [Cours d'essai gratuit]" : "[Cours d'essai gratuit]");
        lesson.setStatus(LessonStatus.PENDING);
        lesson.setIsFromSubscription(false);
        lesson.setPriceCents(0); // Free!

        Lesson savedLesson = lessonRepository.save(lesson);

        // Mark free trial as used
        student.setHasUsedFreeTrial(true);
        userRepository.save(student);

        log.info("Free trial lesson {} booked for student {} with teacher {}",
                savedLesson.getId(), studentId, teacher.getId());

        return LessonResponse.from(savedLesson);
    }

    /**
     * Check if a student is eligible for a free trial lesson.
     */
    public boolean isEligibleForFreeTrial(Long studentId) {
        return userRepository.findById(studentId)
                .map(user -> user.getRole() == UserRole.STUDENT && !Boolean.TRUE.equals(user.getHasUsedFreeTrial()))
                .orElse(false);
    }

    @Transactional
    public LessonResponse updateLessonStatus(Long lessonId, Long userId, UpdateLessonStatusRequest request) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new IllegalArgumentException("Lesson not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        boolean isTeacher = lesson.getTeacher().getId().equals(userId);
        boolean isStudent = lesson.getStudent().getId().equals(userId);

        if (!isTeacher && !isStudent && user.getRole() != UserRole.ADMIN) {
            throw new IllegalArgumentException("Not authorized to update this lesson");
        }

        LessonStatus newStatus = request.status();
        LessonStatus currentStatus = lesson.getStatus();

        validateStatusTransition(currentStatus, newStatus, isTeacher);

        // Handle CONFIRMED - Create video meeting link and calendar events
        if (newStatus == LessonStatus.CONFIRMED && lesson.getZoomLink() == null) {
            // Use Jitsi Meet (free, no auth required) instead of Zoom
            String meetingId = "chessconnect-" + lesson.getId() + "-" + System.currentTimeMillis();
            lesson.setZoomLink("https://meet.jit.si/" + meetingId);
            log.info("Created Jitsi meeting for lesson {}: {}", lesson.getId(), lesson.getZoomLink());

            // Create Google Calendar events for student and teacher (if connected)
            createCalendarEvents(lesson);
        }

        // Handle CANCELLED
        if (newStatus == LessonStatus.CANCELLED) {
            String cancelledBy = isTeacher ? "TEACHER" : "STUDENT";
            handleLessonCancellation(lesson, cancelledBy, request.cancellationReason());
        }

        // Handle COMPLETED - Update student progress and credit teacher earnings
        if (newStatus == LessonStatus.COMPLETED) {
            // Save teacher observations if provided
            if (request.teacherObservations() != null && !request.teacherObservations().isBlank()) {
                lesson.setTeacherObservations(request.teacherObservations());
            }

            // Credit teacher earnings (only if not already credited)
            if (!Boolean.TRUE.equals(lesson.getEarningsCredited())) {
                teacherBalanceService.creditEarningsForCompletedLesson(lesson);
                lesson.setEarningsCredited(true);
            }

            Progress progress = progressRepository.findByStudentId(lesson.getStudent().getId())
                    .orElse(null);
            if (progress != null) {
                progress.recordCompletedLesson();
                progressRepository.save(progress);
            }
        }

        lesson.setStatus(newStatus);
        Lesson updatedLesson = lessonRepository.save(lesson);
        return LessonResponse.from(updatedLesson);
    }

    /**
     * Create Google Calendar events for both student and teacher if they have calendar connected.
     */
    private void createCalendarEvents(Lesson lesson) {
        User student = lesson.getStudent();
        User teacher = lesson.getTeacher();

        // Create event for student if connected
        if (googleCalendarService.isConnected(student)) {
            googleCalendarService.createLessonEvent(lesson, student);
        }

        // Create event for teacher if connected
        if (googleCalendarService.isConnected(teacher)) {
            googleCalendarService.createLessonEvent(lesson, teacher);
        }
    }

    private void createZoomMeeting(Lesson lesson) {
        try {
            ZoomMeetingResponse zoomResponse = zoomService.createMeeting(
                    lesson.getTeacher().getFullName(),
                    lesson.getStudent().getFullName(),
                    lesson.getScheduledAt(),
                    lesson.getDurationMinutes()
            );

            lesson.setZoomMeetingId(String.valueOf(zoomResponse.id()));
            lesson.setZoomLink(zoomResponse.joinUrl());

            log.info("Created Zoom meeting {} for lesson {}",
                    zoomResponse.id(), lesson.getId());

        } catch (Exception e) {
            log.error("Failed to create Zoom meeting for lesson {}: {}",
                    lesson.getId(), e.getMessage());
            // Don't fail the confirmation, just log the error
            // The teacher can manually add a Zoom link later
        }
    }

    public List<LessonResponse> getUpcomingLessonsForStudent(Long studentId) {
        return lessonRepository.findUpcomingLessonsForStudent(studentId, LocalDateTime.now())
                .stream()
                .map(LessonResponse::from)
                .toList();
    }

    public List<LessonResponse> getUpcomingLessonsForTeacher(Long teacherId) {
        return lessonRepository.findUpcomingLessonsForTeacher(teacherId, LocalDateTime.now())
                .stream()
                .map(LessonResponse::from)
                .toList();
    }

    public List<LessonResponse> getLessonHistory(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<Lesson> lessons = user.getRole() == UserRole.TEACHER
                ? lessonRepository.findByTeacherIdOrderByScheduledAtDesc(userId)
                : lessonRepository.findByStudentIdOrderByScheduledAtDesc(userId);

        // History = lessons where the scheduled date has passed
        LocalDateTime now = LocalDateTime.now();
        return lessons.stream()
                .filter(lesson -> {
                    LocalDateTime lessonEnd = lesson.getScheduledAt().plusMinutes(lesson.getDurationMinutes());
                    // Only include in history if the lesson time has passed
                    return lessonEnd.isBefore(now);
                })
                .map(LessonResponse::from)
                .toList();
    }

    public LessonResponse getLessonById(Long lessonId, Long userId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new IllegalArgumentException("Lesson not found"));

        if (!lesson.getStudent().getId().equals(userId) && !lesson.getTeacher().getId().equals(userId)) {
            throw new IllegalArgumentException("Not authorized to view this lesson");
        }

        return LessonResponse.from(lesson);
    }

    @Transactional
    public void deleteLesson(Long lessonId, Long userId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new IllegalArgumentException("Lesson not found"));

        // Only allow deletion by student or teacher involved
        if (!lesson.getStudent().getId().equals(userId) && !lesson.getTeacher().getId().equals(userId)) {
            throw new IllegalArgumentException("Not authorized to delete this lesson");
        }

        // Only allow deletion of completed, cancelled, or no_show lessons
        if (lesson.getStatus() == LessonStatus.PENDING || lesson.getStatus() == LessonStatus.CONFIRMED) {
            throw new IllegalArgumentException("Cannot delete a pending or confirmed lesson. Cancel it first.");
        }

        lessonRepository.delete(lesson);
    }

    private void checkTeacherAvailability(Long teacherId, LocalDateTime scheduledAt, int durationMinutes) {
        LocalDateTime endTime = scheduledAt.plusMinutes(durationMinutes);
        LocalDateTime bufferStart = scheduledAt.minusMinutes(30);
        LocalDateTime bufferEnd = endTime.plusMinutes(30);

        List<Lesson> conflictingLessons = lessonRepository.findTeacherLessonsBetween(
                teacherId, bufferStart, bufferEnd
        );

        boolean hasConflict = conflictingLessons.stream()
                .anyMatch(l -> l.getStatus() == LessonStatus.PENDING || l.getStatus() == LessonStatus.CONFIRMED);

        if (hasConflict) {
            throw new IllegalArgumentException("Teacher is not available at the requested time");
        }
    }

    /**
     * Check if the student has a conflicting lesson at the requested time.
     * Feature 7: Students cannot book overlapping lessons with different teachers.
     */
    private void checkStudentTimeConflict(Long studentId, LocalDateTime scheduledAt, int durationMinutes) {
        LocalDateTime endTime = scheduledAt.plusMinutes(durationMinutes);

        List<Lesson> existingLessons = lessonRepository.findStudentLessonsBetween(
                studentId, scheduledAt.minusMinutes(durationMinutes - 1), endTime
        );

        boolean hasConflict = existingLessons.stream()
                .filter(l -> l.getStatus() == LessonStatus.PENDING || l.getStatus() == LessonStatus.CONFIRMED)
                .anyMatch(existingLesson -> {
                    LocalDateTime existingStart = existingLesson.getScheduledAt();
                    LocalDateTime existingEnd = existingStart.plusMinutes(existingLesson.getDurationMinutes());
                    // Check for overlap
                    return scheduledAt.isBefore(existingEnd) && endTime.isAfter(existingStart);
                });

        if (hasConflict) {
            throw new IllegalArgumentException(
                    "Vous avez deja un cours a cet horaire. Annulez-le d'abord pour pouvoir reserver ce creneau."
            );
        }
    }

    private void validateStatusTransition(LessonStatus current, LessonStatus next, boolean isTeacher) {
        if (current == LessonStatus.COMPLETED || current == LessonStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot update a completed or cancelled lesson");
        }

        if (next == LessonStatus.CONFIRMED && !isTeacher) {
            throw new IllegalArgumentException("Only teachers can confirm lessons");
        }

        if (next == LessonStatus.COMPLETED && !isTeacher) {
            throw new IllegalArgumentException("Only teachers can mark lessons as completed");
        }
    }

    /**
     * Handle lesson cancellation with refund logic.
     * Policy:
     * - Teacher cancels or system auto-cancels: 100% refund
     * - Student cancels > 24h before: 100% refund
     * - Student cancels 2-24h before: 50% refund
     * - Student cancels < 2h before: 0% refund
     */
    private void handleLessonCancellation(Lesson lesson, String cancelledBy, String reason) {
        lesson.setCancellationReason(reason);
        lesson.setCancelledBy(cancelledBy);
        lesson.setCancelledAt(LocalDateTime.now());

        // Delete video meeting if exists
        if (lesson.getZoomMeetingId() != null) {
            zoomService.deleteMeeting(lesson.getZoomMeetingId());
            lesson.setZoomLink(null);
            lesson.setZoomMeetingId(null);
        }

        // Handle refund based on who cancelled and timing
        if (lesson.getIsFromSubscription()) {
            // Subscription-based lesson: restore quota
            handleSubscriptionCancellation(lesson, cancelledBy);
        } else {
            // Paid lesson: process refund
            handlePaidLessonCancellation(lesson, cancelledBy);
        }
    }

    /**
     * Handle cancellation for subscription-based lessons.
     * Always restore quota regardless of timing (lesson already "paid" via subscription).
     */
    private void handleSubscriptionCancellation(Lesson lesson, String cancelledBy) {
        Subscription sub = lesson.getSubscription();
        if (sub != null) {
            // Student cancels < 2h before: don't restore quota (lesson counted as used)
            if ("STUDENT".equals(cancelledBy)) {
                long hoursUntilLesson = ChronoUnit.HOURS.between(LocalDateTime.now(), lesson.getScheduledAt());
                if (hoursUntilLesson < PARTIAL_REFUND_HOURS) {
                    log.info("Late cancellation by student - subscription quota not restored for lesson {}",
                            lesson.getId());
                    lesson.setRefundPercentage(0);
                    return;
                }
            }

            // Restore the lesson to quota
            sub.setLessonsUsedThisMonth(Math.max(0, sub.getLessonsUsedThisMonth() - 1));
            lesson.setRefundPercentage(100);
            log.info("Subscription quota restored for lesson {} (cancelled by {})",
                    lesson.getId(), cancelledBy);
        }
    }

    /**
     * Handle cancellation for paid lessons with Stripe refund.
     */
    private void handlePaidLessonCancellation(Lesson lesson, String cancelledBy) {
        // Calculate refund percentage
        int refundPercentage = calculateRefundPercentage(lesson, cancelledBy);
        lesson.setRefundPercentage(refundPercentage);

        if (refundPercentage == 0) {
            log.info("No refund for lesson {} (late cancellation by student)", lesson.getId());
            return;
        }

        // Find the payment for this lesson
        Payment payment = paymentRepository.findByLessonId(lesson.getId()).orElse(null);
        if (payment == null || payment.getStripePaymentIntentId() == null) {
            log.warn("No payment found for lesson {} - cannot process refund", lesson.getId());
            return;
        }

        // Process Stripe refund
        try {
            int refundAmount = (lesson.getPriceCents() * refundPercentage) / 100;
            String refundReason = String.format("Lesson cancelled by %s - %d%% refund",
                    cancelledBy.toLowerCase(), refundPercentage);

            Refund refund = stripeService.createPartialRefund(
                    payment.getStripePaymentIntentId(),
                    lesson.getPriceCents(),
                    refundPercentage,
                    refundReason
            );

            if (refund != null) {
                lesson.setStripeRefundId(refund.getId());
                lesson.setRefundedAmountCents(refundAmount);
                payment.setStatus(PaymentStatus.REFUNDED);
                payment.setRefundReason(refundReason);
                paymentRepository.save(payment);

                log.info("Refund processed for lesson {}: {} cents ({}%)",
                        lesson.getId(), refundAmount, refundPercentage);
            }
        } catch (Exception e) {
            log.error("Failed to process refund for lesson {}: {}", lesson.getId(), e.getMessage());
            // Don't fail the cancellation, but log the error
        }
    }

    /**
     * Calculate refund percentage based on cancellation policy.
     */
    private int calculateRefundPercentage(Lesson lesson, String cancelledBy) {
        // Teacher or system cancellation: always 100% refund
        if ("TEACHER".equals(cancelledBy) || "SYSTEM".equals(cancelledBy)) {
            return 100;
        }

        // Student cancellation: depends on timing
        long hoursUntilLesson = ChronoUnit.HOURS.between(LocalDateTime.now(), lesson.getScheduledAt());

        if (hoursUntilLesson >= FULL_REFUND_HOURS) {
            return 100; // > 24h before: full refund
        } else if (hoursUntilLesson >= PARTIAL_REFUND_HOURS) {
            return 50;  // 2-24h before: 50% refund
        } else {
            return 0;   // < 2h before: no refund
        }
    }

    /**
     * Auto-cancel lessons where teacher didn't confirm within 24h.
     * Runs every hour.
     */
    @Scheduled(cron = "0 0 * * * *") // Every hour at minute 0
    @Transactional
    public void autoCancelUnconfirmedLessons() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(TEACHER_CONFIRMATION_HOURS);

        List<Lesson> unconfirmedLessons = lessonRepository.findByStatusAndCreatedAtBefore(
                LessonStatus.PENDING, cutoffTime
        );

        for (Lesson lesson : unconfirmedLessons) {
            log.info("Auto-cancelling unconfirmed lesson {} (created at {})",
                    lesson.getId(), lesson.getCreatedAt());

            handleLessonCancellation(lesson, "SYSTEM",
                    "Annulation automatique - le professeur n'a pas confirmé dans les 24h");
            lesson.setStatus(LessonStatus.CANCELLED);
            lessonRepository.save(lesson);
        }

        if (!unconfirmedLessons.isEmpty()) {
            log.info("Auto-cancelled {} unconfirmed lessons", unconfirmedLessons.size());
        }
    }
}
