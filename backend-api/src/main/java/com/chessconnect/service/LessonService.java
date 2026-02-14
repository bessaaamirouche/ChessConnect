package com.chessconnect.service;

import com.chessconnect.dto.lesson.BookLessonRequest;
import com.chessconnect.dto.lesson.LessonResponse;
import com.chessconnect.dto.lesson.UpdateLessonStatusRequest;
import com.chessconnect.event.NotificationEvent;
import com.chessconnect.event.payload.LessonStatusPayload;
import com.chessconnect.event.payload.TeacherJoinedPayload;
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
import com.chessconnect.repository.GroupInvitationRepository;
import com.chessconnect.repository.InvoiceRepository;
import com.chessconnect.repository.LessonParticipantRepository;
import com.chessconnect.repository.LessonRepository;
import com.chessconnect.repository.PaymentRepository;
import com.chessconnect.repository.ProgressRepository;
import com.chessconnect.repository.RatingRepository;
import com.chessconnect.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Service
public class LessonService {

    private static final Logger log = LoggerFactory.getLogger(LessonService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Cancellation policy constants
    private static final int FULL_REFUND_HOURS = 24;       // > 24h before = 100% refund
    private static final int PARTIAL_REFUND_HOURS = 2;     // 2-24h before = 50% refund
    private static final int TEACHER_CONFIRMATION_HOURS = 24; // Teacher must confirm within 24h

    private final LessonRepository lessonRepository;
    private final LessonParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final ProgressRepository progressRepository;
    private final PaymentRepository paymentRepository;
    private final RatingRepository ratingRepository;
    private final InvoiceRepository invoiceRepository;
    private final CreditTransactionRepository creditTransactionRepository;
    private final CourseRepository courseRepository;
    private final TeacherBalanceService teacherBalanceService;
    private final InvoiceService invoiceService;
    private final WalletService walletService;
    private final ProgrammeService programmeService;
    private final PendingValidationService pendingValidationService;
    private final BunnyStorageService bunnyStorageService;
    private final GroupInvitationRepository groupInvitationRepository;
    private final ApplicationEventPublisher eventPublisher;
    private GroupLessonService groupLessonService;

    public LessonService(
            LessonRepository lessonRepository,
            LessonParticipantRepository participantRepository,
            UserRepository userRepository,
            ProgressRepository progressRepository,
            PaymentRepository paymentRepository,
            RatingRepository ratingRepository,
            InvoiceRepository invoiceRepository,
            CreditTransactionRepository creditTransactionRepository,
            CourseRepository courseRepository,
            TeacherBalanceService teacherBalanceService,
            InvoiceService invoiceService,
            WalletService walletService,
            ProgrammeService programmeService,
            PendingValidationService pendingValidationService,
            BunnyStorageService bunnyStorageService,
            GroupInvitationRepository groupInvitationRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.lessonRepository = lessonRepository;
        this.participantRepository = participantRepository;
        this.userRepository = userRepository;
        this.progressRepository = progressRepository;
        this.paymentRepository = paymentRepository;
        this.ratingRepository = ratingRepository;
        this.invoiceRepository = invoiceRepository;
        this.creditTransactionRepository = creditTransactionRepository;
        this.courseRepository = courseRepository;
        this.teacherBalanceService = teacherBalanceService;
        this.invoiceService = invoiceService;
        this.walletService = walletService;
        this.programmeService = programmeService;
        this.pendingValidationService = pendingValidationService;
        this.bunnyStorageService = bunnyStorageService;
        this.groupInvitationRepository = groupInvitationRepository;
        this.eventPublisher = eventPublisher;
    }

    // Setter injection to break circular dependency with GroupLessonService
    @org.springframework.beans.factory.annotation.Autowired
    public void setGroupLessonService(GroupLessonService groupLessonService) {
        this.groupLessonService = groupLessonService;
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

        // Publish SSE event to notify teacher of new booking
        publishLessonBookedEvent(savedLesson);

        return LessonResponse.from(savedLesson);
    }

    /**
     * Publish SSE event when a lesson is booked.
     */
    private void publishLessonBookedEvent(Lesson lesson) {
        try {
            String studentName = lesson.getStudent().getFirstName() + " " + lesson.getStudent().getLastName();
            String teacherName = lesson.getTeacher().getFirstName() + " " + lesson.getTeacher().getLastName();

            LessonStatusPayload payload = new LessonStatusPayload(
                    lesson.getId(),
                    null,
                    LessonStatus.PENDING.name(),
                    teacherName,
                    studentName,
                    lesson.getScheduledAt().format(DATE_FORMATTER)
            );

            // Notify teacher
            eventPublisher.publishEvent(new NotificationEvent(
                    this,
                    NotificationEvent.EventType.LESSON_BOOKED,
                    lesson.getTeacher().getId(),
                    payload
            ));
        } catch (Exception e) {
            log.warn("Failed to publish SSE lesson booked event: {}", e.getMessage());
        }
    }

    @Transactional
    public LessonResponse updateLessonStatus(Long lessonId, Long userId, UpdateLessonStatusRequest request) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new IllegalArgumentException("Lesson not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        boolean isTeacher = lesson.getTeacher().getId().equals(userId);
        boolean isStudent = lesson.getStudent().getId().equals(userId);

        // For group lessons, check if user is a participant
        if (!isStudent && Boolean.TRUE.equals(lesson.getIsGroupLesson())) {
            isStudent = participantRepository.existsActiveByLessonIdAndStudentId(lessonId, userId);
        }

        if (!isTeacher && !isStudent && user.getRole() != UserRole.ADMIN) {
            throw new IllegalArgumentException("Not authorized to update this lesson");
        }

        LessonStatus newStatus = request.status();
        LessonStatus currentStatus = lesson.getStatus();

        validateStatusTransition(currentStatus, newStatus, isTeacher);

        // Handle CONFIRMED - Create video meeting link
        if (newStatus == LessonStatus.CONFIRMED && lesson.getZoomLink() == null) {
            // Use Jitsi Meet (free, no auth required) instead of Zoom
            String meetingId = "chessconnect-" + lesson.getId() + "-" + System.currentTimeMillis();
            lesson.setZoomLink("https://meet.mychess.fr/" + meetingId);
            log.info("Created Jitsi meeting for lesson {}: {}", lesson.getId(), lesson.getZoomLink());
        }

        // Handle CANCELLED
        if (newStatus == LessonStatus.CANCELLED) {
            // Prevent cancellation if lesson has already started
            if (lesson.getScheduledAt().isBefore(LocalDateTime.now())) {
                throw new IllegalArgumentException("Impossible d'annuler un cours deja commence");
            }
            String cancelledBy = isTeacher ? "TEACHER" : "STUDENT";

            // For group lessons cancelled by teacher, refund all participants
            if (Boolean.TRUE.equals(lesson.getIsGroupLesson()) && isTeacher && groupLessonService != null) {
                groupLessonService.cancelGroupByTeacher(lesson, request.cancellationReason());
            }

            handleLessonCancellation(lesson, cancelledBy, request.cancellationReason());
        }

        // Handle COMPLETED - Update student progress and credit teacher earnings
        if (newStatus == LessonStatus.COMPLETED) {
            // Save teacher observations if provided
            if (request.teacherObservations() != null && !request.teacherObservations().isBlank()) {
                lesson.setTeacherObservations(request.teacherObservations());
            }

            // For group lessons, use group-specific completion logic
            if (Boolean.TRUE.equals(lesson.getIsGroupLesson()) && groupLessonService != null) {
                groupLessonService.handleGroupLessonCompletion(lesson);
            } else {
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

            // Advance student to next course in the programme
            try {
                programmeService.advanceToNextCourse(lesson.getStudent().getId());
                log.info("Advanced student {} to next course after completing lesson {}",
                        lesson.getStudent().getId(), lesson.getId());
            } catch (Exception e) {
                log.warn("Could not advance student to next course: {}", e.getMessage());
            }

            // Create pending validation for the coach to validate the student's courses
            try {
                pendingValidationService.createPendingValidation(
                        lesson.getId(),
                        lesson.getTeacher().getId(),
                        lesson.getStudent().getId()
                );
            } catch (Exception e) {
                log.warn("Could not create pending validation: {}", e.getMessage());
            }
        }

        LessonStatus oldStatus = lesson.getStatus();
        lesson.setStatus(newStatus);
        Lesson updatedLesson = lessonRepository.save(lesson);

        // Publish SSE event for status change
        if (newStatus == LessonStatus.CONFIRMED || newStatus == LessonStatus.CANCELLED) {
            publishLessonStatusChangedEvent(updatedLesson, oldStatus, newStatus);
        }

        return LessonResponse.from(updatedLesson);
    }

    /**
     * Publish SSE event when a lesson status changes.
     */
    private void publishLessonStatusChangedEvent(Lesson lesson, LessonStatus oldStatus, LessonStatus newStatus) {
        try {
            String studentName = lesson.getStudent().getFirstName() + " " + lesson.getStudent().getLastName();
            String teacherName = lesson.getTeacher().getFirstName() + " " + lesson.getTeacher().getLastName();

            LessonStatusPayload payload = new LessonStatusPayload(
                    lesson.getId(),
                    oldStatus.name(),
                    newStatus.name(),
                    teacherName,
                    studentName,
                    lesson.getScheduledAt().format(DATE_FORMATTER)
            );

            // For group lessons, notify all participants
            if (Boolean.TRUE.equals(lesson.getIsGroupLesson())) {
                var participants = participantRepository.findByLessonIdAndStatus(lesson.getId(), "ACTIVE");
                for (var participant : participants) {
                    eventPublisher.publishEvent(new NotificationEvent(
                            this,
                            NotificationEvent.EventType.LESSON_STATUS_CHANGED,
                            participant.getStudent().getId(),
                            payload
                    ));
                }
            } else {
                // Notify student
                eventPublisher.publishEvent(new NotificationEvent(
                        this,
                        NotificationEvent.EventType.LESSON_STATUS_CHANGED,
                        lesson.getStudent().getId(),
                        payload
                ));
            }

            // Notify teacher
            eventPublisher.publishEvent(new NotificationEvent(
                    this,
                    NotificationEvent.EventType.LESSON_STATUS_CHANGED,
                    lesson.getTeacher().getId(),
                    payload
            ));
        } catch (Exception e) {
            log.warn("Failed to publish SSE lesson status changed event: {}", e.getMessage());
        }
    }

    public List<LessonResponse> getUpcomingLessonsForStudent(Long studentId) {
        // Private lessons where student is the direct student
        List<Lesson> privateLessons = lessonRepository.findUpcomingLessonsForStudent(studentId, LocalDateTime.now());

        // Group lessons where student is a participant
        List<Lesson> groupLessons = participantRepository.findUpcomingGroupLessonsForStudent(studentId);

        return Stream.concat(privateLessons.stream(), groupLessons.stream())
                .distinct()
                .sorted(Comparator.comparing(Lesson::getScheduledAt))
                .map(this::toLessonResponseWithToken)
                .toList();
    }

    public List<LessonResponse> getUpcomingLessonsForTeacher(Long teacherId) {
        return lessonRepository.findUpcomingLessonsForTeacher(teacherId, LocalDateTime.now())
                .stream()
                .map(this::toLessonResponseWithToken)
                .toList();
    }

    private LessonResponse toLessonResponseWithToken(Lesson lesson) {
        LessonResponse response = LessonResponse.from(lesson);
        if (Boolean.TRUE.equals(lesson.getIsGroupLesson())) {
            return groupInvitationRepository.findByLessonId(lesson.getId())
                    .map(inv -> response.withInvitationToken(inv.getToken()))
                    .orElse(response);
        }
        return response;
    }

    public List<LessonResponse> getLessonHistory(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        boolean isTeacher = user.getRole() == UserRole.TEACHER;
        List<Lesson> lessons = isTeacher
                ? lessonRepository.findByTeacherIdOrderByScheduledAtDesc(userId)
                : lessonRepository.findByStudentIdOrderByScheduledAtDesc(userId);

        // For students, also include group lessons where they were a participant
        if (!isTeacher) {
            // Exclude group lessons where the student cancelled their participation
            final Long studentId = userId;
            lessons = lessons.stream()
                    .filter(lesson -> !Boolean.TRUE.equals(lesson.getIsGroupLesson())
                            || participantRepository.existsActiveByLessonIdAndStudentId(lesson.getId(), studentId))
                    .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));

            List<Lesson> groupHistory = participantRepository.findHistoryGroupLessonsForStudent(userId);
            lessons = Stream.concat(lessons.stream(), groupHistory.stream())
                    .distinct()
                    .sorted(Comparator.comparing(Lesson::getScheduledAt).reversed())
                    .toList();
        }

        // History = lessons where the scheduled date has passed, excluding soft-deleted ones
        LocalDateTime now = LocalDateTime.now();
        return lessons.stream()
                .filter(lesson -> {
                    LocalDateTime lessonEnd = lesson.getScheduledAt().plusMinutes(lesson.getDurationMinutes());
                    // Only include in history if the lesson time has passed
                    if (!lessonEnd.isBefore(now)) return false;
                    // Filter out soft-deleted lessons based on user role
                    if (isTeacher && Boolean.TRUE.equals(lesson.getDeletedByTeacher())) return false;
                    if (!isTeacher && Boolean.TRUE.equals(lesson.getDeletedByStudent())) return false;
                    return true;
                })
                .map(LessonResponse::from)
                .toList();
    }

    public LessonResponse getLessonById(Long lessonId, Long userId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new IllegalArgumentException("Lesson not found"));

        boolean authorized = lesson.getStudent().getId().equals(userId)
                || lesson.getTeacher().getId().equals(userId);

        // For group lessons, also check participants table
        if (!authorized && Boolean.TRUE.equals(lesson.getIsGroupLesson())) {
            authorized = participantRepository.existsActiveByLessonIdAndStudentId(lessonId, userId);
        }

        if (!authorized) {
            throw new IllegalArgumentException("Not authorized to view this lesson");
        }

        return LessonResponse.from(lesson);
    }

    /**
     * Add or update a teacher comment on a completed lesson.
     * Only the teacher can add comments, and only on lessons that are still in their history.
     */
    @Transactional
    public LessonResponse updateTeacherComment(Long lessonId, Long teacherId, String comment) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new IllegalArgumentException("Lesson not found"));

        // Only the teacher of this lesson can add comments
        if (!lesson.getTeacher().getId().equals(teacherId)) {
            throw new IllegalArgumentException("Not authorized to comment on this lesson");
        }

        // Only allow comments on completed or cancelled lessons (history)
        if (lesson.getStatus() == LessonStatus.PENDING || lesson.getStatus() == LessonStatus.CONFIRMED) {
            throw new IllegalArgumentException("Cannot add comment to a pending or confirmed lesson");
        }

        // Cannot add comment if teacher has soft-deleted this lesson
        if (Boolean.TRUE.equals(lesson.getDeletedByTeacher())) {
            throw new IllegalArgumentException("Cannot add comment to a deleted lesson");
        }

        lesson.setTeacherComment(comment);
        lesson.setTeacherCommentAt(LocalDateTime.now());
        Lesson updatedLesson = lessonRepository.save(lesson);

        log.info("Teacher {} updated comment on lesson {}", teacherId, lessonId);
        return LessonResponse.from(updatedLesson);
    }

    @Transactional
    public void deleteLesson(Long lessonId, Long userId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new IllegalArgumentException("Lesson not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Only allow deletion by student or teacher involved
        boolean isTeacher = lesson.getTeacher().getId().equals(userId);
        boolean isStudent = lesson.getStudent().getId().equals(userId);

        // For group lessons, also check if user is a participant
        if (!isStudent && Boolean.TRUE.equals(lesson.getIsGroupLesson())) {
            isStudent = participantRepository.existsByLessonIdAndStudentId(lesson.getId(), userId);
        }

        if (!isTeacher && !isStudent) {
            throw new IllegalArgumentException("Not authorized to delete this lesson");
        }

        // Only allow deletion of completed, cancelled, or no_show lessons
        if (lesson.getStatus() == LessonStatus.PENDING || lesson.getStatus() == LessonStatus.CONFIRMED) {
            throw new IllegalArgumentException("Cannot delete a pending or confirmed lesson. Cancel it first.");
        }

        // Soft delete: mark as deleted for the current user only
        // The lesson remains visible to the other party
        if (isTeacher) {
            lesson.setDeletedByTeacher(true);
            log.info("Soft deleted lesson {} for teacher {}", lessonId, userId);
        } else {
            lesson.setDeletedByStudent(true);
            log.info("Soft deleted lesson {} for student {}", lessonId, userId);
        }

        lessonRepository.save(lesson);

        // Only perform hard delete if both parties have deleted the lesson
        if (Boolean.TRUE.equals(lesson.getDeletedByTeacher()) && Boolean.TRUE.equals(lesson.getDeletedByStudent())) {
            performHardDelete(lesson);
        }
    }

    /**
     * Perform hard delete of a lesson when both parties have soft-deleted it.
     */
    private void performHardDelete(Lesson lesson) {
        Long lessonId = lesson.getId();
        log.info("Performing hard delete of lesson {} (both parties deleted)", lessonId);

        // Delete recording from Bunny CDN if applicable
        deleteCdnRecording(lesson);

        // Delete local recording files if they exist
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
        log.info("Hard deleted lesson {}", lessonId);
    }

    /**
     * Delete recording from Bunny CDN if the recording URL is from Bunny.
     */
    private void deleteCdnRecording(Lesson lesson) {
        String recordingUrl = lesson.getRecordingUrl();
        if (recordingUrl == null || recordingUrl.isBlank()) {
            return;
        }

        // Check if URL is from Bunny CDN
        if (bunnyStorageService.isBunnyCdnUrl(recordingUrl)) {
            String filename = bunnyStorageService.extractFilenameFromUrl(recordingUrl);
            if (filename != null) {
                boolean deleted = bunnyStorageService.deleteRecording(filename);
                if (deleted) {
                    log.info("Deleted recording from Bunny CDN for lesson {}: {}", lesson.getId(), filename);
                } else {
                    log.warn("Failed to delete recording from Bunny CDN for lesson {}: {}", lesson.getId(), filename);
                }
            }
        }
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

        LocalDateTime now = LocalDateTime.now();
        lesson.setTeacherJoinedAt(now);

        lessonRepository.save(lesson);

        // Publish SSE event to notify student that teacher has joined
        publishTeacherJoinedEvent(lesson);

        return LessonResponse.from(lesson);
    }

    /**
     * Publish SSE event when teacher joins the video call.
     */
    private void publishTeacherJoinedEvent(Lesson lesson) {
        try {
            String teacherName = lesson.getTeacher().getFirstName() + " " + lesson.getTeacher().getLastName();

            TeacherJoinedPayload payload = new TeacherJoinedPayload(
                    lesson.getId(),
                    teacherName
            );

            // Notify student
            eventPublisher.publishEvent(new NotificationEvent(
                    this,
                    NotificationEvent.EventType.TEACHER_JOINED_CALL,
                    lesson.getStudent().getId(),
                    payload
            ));
        } catch (Exception e) {
            log.warn("Failed to publish SSE teacher joined event: {}", e.getMessage());
        }
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

        // For group lessons, refunds are handled by cancelGroupByTeacher/cancelParticipant
        // Skip handlePaidLessonCancellation to avoid NonUniqueResultException
        // (group lessons have multiple payments per lesson)
        if (Boolean.TRUE.equals(lesson.getIsGroupLesson())) {
            return;
        }

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
     * Check if this is the first completed lesson between a teacher and student.
     * Used to determine whether to show level evaluation modal or course validation modal.
     */
    public Map<String, Object> checkFirstLesson(Long teacherId, Long studentId) {
        Map<String, Object> result = new HashMap<>();

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));

        // Check if student's level has been set by a coach
        Progress progress = progressRepository.findByStudentId(studentId).orElse(null);
        boolean levelSetByCoach = progress != null && Boolean.TRUE.equals(progress.getLevelSetByCoach());

        // If level was already set by a coach, it's not a "first lesson" in terms of evaluation
        result.put("isFirstLesson", !levelSetByCoach);
        result.put("studentName", student.getFirstName() + " " + student.getLastName());
        result.put("studentId", studentId);

        return result;
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
