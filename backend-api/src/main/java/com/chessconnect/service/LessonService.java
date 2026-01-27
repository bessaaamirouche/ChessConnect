package com.chessconnect.service;

import com.chessconnect.dto.lesson.BookLessonRequest;
import com.chessconnect.dto.lesson.LessonResponse;
import com.chessconnect.dto.lesson.UpdateLessonStatusRequest;
import com.chessconnect.model.Lesson;
import com.chessconnect.model.Payment;
import com.chessconnect.model.Progress;
import com.chessconnect.model.User;
import com.chessconnect.model.enums.LessonStatus;
import com.chessconnect.model.enums.PaymentStatus;
import com.chessconnect.model.enums.PaymentType;
import com.chessconnect.model.enums.UserRole;
import com.chessconnect.model.Course;
import com.chessconnect.repository.CourseRepository;
import com.chessconnect.repository.CreditTransactionRepository;
import com.chessconnect.repository.InvoiceRepository;
import com.chessconnect.repository.LessonRepository;
import com.chessconnect.repository.PaymentRepository;
import com.chessconnect.repository.ProgressRepository;
import com.chessconnect.repository.RatingRepository;
import com.chessconnect.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
public class LessonService {

    private static final Logger log = LoggerFactory.getLogger(LessonService.class);

    // Cancellation policy constants
    private static final int FULL_REFUND_HOURS = 24;       // > 24h before = 100% refund
    private static final int PARTIAL_REFUND_HOURS = 2;     // 2-24h before = 50% refund
    private static final int TEACHER_CONFIRMATION_HOURS = 24; // Teacher must confirm within 24h

    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;
    private final ProgressRepository progressRepository;
    private final PaymentRepository paymentRepository;
    private final RatingRepository ratingRepository;
    private final InvoiceRepository invoiceRepository;
    private final CreditTransactionRepository creditTransactionRepository;
    private final CourseRepository courseRepository;
    private final TeacherBalanceService teacherBalanceService;
    private final GoogleCalendarService googleCalendarService;
    private final InvoiceService invoiceService;
    private final WalletService walletService;

    public LessonService(
            LessonRepository lessonRepository,
            UserRepository userRepository,
            ProgressRepository progressRepository,
            PaymentRepository paymentRepository,
            RatingRepository ratingRepository,
            InvoiceRepository invoiceRepository,
            CreditTransactionRepository creditTransactionRepository,
            CourseRepository courseRepository,
            TeacherBalanceService teacherBalanceService,
            GoogleCalendarService googleCalendarService,
            InvoiceService invoiceService,
            WalletService walletService
    ) {
        this.lessonRepository = lessonRepository;
        this.userRepository = userRepository;
        this.progressRepository = progressRepository;
        this.paymentRepository = paymentRepository;
        this.ratingRepository = ratingRepository;
        this.invoiceRepository = invoiceRepository;
        this.creditTransactionRepository = creditTransactionRepository;
        this.courseRepository = courseRepository;
        this.teacherBalanceService = teacherBalanceService;
        this.googleCalendarService = googleCalendarService;
        this.invoiceService = invoiceService;
        this.walletService = walletService;
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

        // Set the course if provided
        if (request.courseId() != null) {
            Course course = courseRepository.findById(request.courseId())
                    .orElse(null);
            lesson.setCourse(course);
        }

        // All lessons are paid at the coach's hourly rate
        lesson.setIsFromSubscription(false);
        lesson.setPriceCents(teacher.getHourlyRateCents());

        Lesson savedLesson = lessonRepository.save(lesson);

        // If student was eligible for free trial but is paying directly, forfeit the free trial
        if (!Boolean.TRUE.equals(student.getHasUsedFreeTrial())) {
            student.setHasUsedFreeTrial(true);
            userRepository.save(student);
            log.info("Student {} forfeited free trial by paying for lesson {}", studentId, savedLesson.getId());
        }

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

        // Check if teacher accepts free trial lessons
        if (!Boolean.TRUE.equals(teacher.getAcceptsFreeTrial())) {
            throw new IllegalArgumentException("Ce coach n'accepte pas les cours découverte gratuits");
        }

        // Validate that the lesson end time is still in the future
        LocalDateTime lessonEnd = request.scheduledAt().plusMinutes(request.durationMinutes());
        if (!lessonEnd.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Lesson must end in the future");
        }

        checkTeacherAvailability(teacher.getId(), request.scheduledAt(), request.durationMinutes());
        checkStudentTimeConflict(studentId, request.scheduledAt(), request.durationMinutes());

        // Create the free trial lesson (15 minutes discovery session)
        Lesson lesson = new Lesson();
        lesson.setStudent(student);
        lesson.setTeacher(teacher);
        lesson.setScheduledAt(request.scheduledAt());
        lesson.setDurationMinutes(15); // Free trial is limited to 15 minutes
        lesson.setNotes("[Cours découverte - 15 min]");
        lesson.setStatus(LessonStatus.PENDING);
        lesson.setIsFromSubscription(false);
        lesson.setIsFreeTrial(true); // Mark as free trial
        lesson.setPriceCents(0); // Free!

        // Set the course if provided
        if (request.courseId() != null) {
            Course course = courseRepository.findById(request.courseId())
                    .orElse(null);
            lesson.setCourse(course);
        }

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
            lesson.setZoomLink("https://meet.mychess.fr/" + meetingId);
            log.info("Created Jitsi meeting for lesson {}: {}", lesson.getId(), lesson.getZoomLink());

            // Create Google Calendar events for student and teacher (if connected)
            createCalendarEvents(lesson);
        }

        // Handle CANCELLED
        if (newStatus == LessonStatus.CANCELLED) {
            // Prevent cancellation if lesson has already started
            if (lesson.getScheduledAt().isBefore(LocalDateTime.now())) {
                throw new IllegalArgumentException("Impossible d'annuler un cours deja commence");
            }
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

        // Delete recording files if they exist
        deleteRecordingFiles(lessonId);

        // Delete related rating if exists
        ratingRepository.findByLessonId(lessonId).ifPresent(rating -> {
            log.info("Deleting rating {} for lesson {}", rating.getId(), lessonId);
            ratingRepository.delete(rating);
        });

        // Nullify lesson reference in invoices (keep invoices for legal/accounting purposes)
        invoiceRepository.findByLessonId(lessonId).forEach(invoice -> {
            log.info("Nullifying lesson reference in invoice {}", invoice.getId());
            invoice.setLesson(null);
            invoiceRepository.save(invoice);
        });

        // Nullify lesson reference in credit transactions (keep for accounting history)
        creditTransactionRepository.findByLessonId(lessonId).forEach(transaction -> {
            log.info("Nullifying lesson reference in credit transaction {}", transaction.getId());
            transaction.setLesson(null);
            creditTransactionRepository.save(transaction);
        });

        // Delete related payment if exists
        paymentRepository.findByLessonId(lessonId).ifPresent(payment -> {
            log.info("Deleting payment {} for lesson {}", payment.getId(), lessonId);
            paymentRepository.delete(payment);
        });

        lessonRepository.delete(lesson);
        log.info("Deleted lesson {} by user {}", lessonId, userId);
    }

    /**
     * Delete recording files associated with a lesson.
     * Recordings are stored in /var/jibri/recordings/ with directory names like:
     * - mychess-lesson-{id}
     * - chessconnect-{id}-{timestamp}
     * - Lesson-{id}
     */
    private void deleteRecordingFiles(Long lessonId) {
        String recordingsBasePath = "/var/jibri/recordings";
        File baseDir = new File(recordingsBasePath);

        if (!baseDir.exists() || !baseDir.isDirectory()) {
            log.debug("Recordings directory does not exist: {}", recordingsBasePath);
            return;
        }

        // Find directories matching the lesson ID
        File[] matchingDirs = baseDir.listFiles((dir, name) -> {
            if (name.equals("Lesson-" + lessonId)) return true;
            if (name.equals("ChessConnect_Lesson-" + lessonId)) return true;
            if (name.startsWith("chessconnect-" + lessonId + "-")) return true;
            if (name.equals("mychess-lesson-" + lessonId)) return true;
            return false;
        });

        if (matchingDirs == null || matchingDirs.length == 0) {
            log.debug("No recording directories found for lesson {}", lessonId);
            return;
        }

        for (File dir : matchingDirs) {
            try {
                // Delete directory and all its contents recursively
                try (Stream<Path> pathStream = Files.walk(dir.toPath())) {
                    pathStream.sorted(Comparator.reverseOrder())
                            .map(Path::toFile)
                            .forEach(File::delete);
                }
                log.info("Deleted recording directory for lesson {}: {}", lessonId, dir.getAbsolutePath());
            } catch (IOException e) {
                log.error("Failed to delete recording directory for lesson {}: {}", lessonId, e.getMessage());
            }
        }
    }

    /**
     * Mark that the teacher has joined the video call for this lesson.
     * This enables the student to join as well.
     */
    @Transactional
    public LessonResponse markTeacherJoined(Long lessonId, Long teacherId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new IllegalArgumentException("Lesson not found"));

        // Verify the user is the teacher for this lesson
        if (!lesson.getTeacher().getId().equals(teacherId)) {
            throw new IllegalArgumentException("Only the assigned teacher can mark as joined");
        }

        // Only allow for confirmed lessons
        if (lesson.getStatus() != LessonStatus.CONFIRMED) {
            throw new IllegalArgumentException("Can only join confirmed lessons");
        }

        lesson.setTeacherJoinedAt(LocalDateTime.now());
        lessonRepository.save(lesson);

        return LessonResponse.from(lesson);
    }

    /**
     * Check if the teacher has joined the video call for this lesson.
     */
    public boolean hasTeacherJoined(Long lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new IllegalArgumentException("Lesson not found"));
        return lesson.getTeacherJoinedAt() != null;
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

        // Process refund for paid lessons
        handlePaidLessonCancellation(lesson, cancelledBy);
    }

    /**
     * Handle cancellation for paid lessons with refund.
     * All refunds are credited to the student's wallet (no Stripe refunds).
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
        if (payment == null) {
            log.warn("No payment found for lesson {} - cannot process refund", lesson.getId());
            return;
        }

        int refundAmount = (lesson.getPriceCents() * refundPercentage) / 100;
        String refundReason = String.format("Lesson cancelled by %s - %d%% refund",
                cancelledBy.toLowerCase(), refundPercentage);

        // All refunds go to wallet (no Stripe refunds)
        try {
            walletService.refundCreditForLesson(
                    lesson.getStudent().getId(),
                    lesson,
                    lesson.getPriceCents(),
                    refundPercentage
            );

            lesson.setRefundedAmountCents(refundAmount);
            payment.setStatus(PaymentStatus.REFUNDED);
            payment.setRefundReason(refundReason);
            paymentRepository.save(payment);

            log.info("Wallet refund processed for lesson {}: {} cents ({}%)",
                    lesson.getId(), refundAmount, refundPercentage);

            // Generate credit note for wallet refund
            invoiceService.generateCreditNoteForWalletRefund(lesson, refundPercentage, refundAmount);
        } catch (Exception e) {
            log.error("Failed to process wallet refund for lesson {}: {}", lesson.getId(), e.getMessage());
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
                    "Annulation automatique - le coach n'a pas confirmé dans les 24h");
            lesson.setStatus(LessonStatus.CANCELLED);
            lessonRepository.save(lesson);
        }

        if (!unconfirmedLessons.isEmpty()) {
            log.info("Auto-cancelled {} unconfirmed lessons", unconfirmedLessons.size());
        }
    }
}
