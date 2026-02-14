package com.chessconnect.service;

import com.chessconnect.dto.group.*;
import com.chessconnect.dto.lesson.BookLessonRequest;
import com.chessconnect.dto.lesson.LessonResponse;
import com.chessconnect.event.NotificationEvent;
import com.chessconnect.event.payload.LessonStatusPayload;
import com.chessconnect.model.*;
import com.chessconnect.model.enums.*;
import com.chessconnect.model.Availability;
import com.chessconnect.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class GroupLessonService {

    private static final Logger log = LoggerFactory.getLogger(GroupLessonService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final int FULL_REFUND_HOURS = 24;
    private static final int PARTIAL_REFUND_HOURS = 2;

    private final LessonRepository lessonRepository;
    private final LessonParticipantRepository participantRepository;
    private final GroupInvitationRepository invitationRepository;
    private final AvailabilityRepository availabilityRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final PaymentRepository paymentRepository;
    private final WalletService walletService;
    private final InvoiceService invoiceService;
    private final TeacherBalanceService teacherBalanceService;
    private final ProgressRepository progressRepository;
    private final ApplicationEventPublisher eventPublisher;

    public GroupLessonService(
            LessonRepository lessonRepository,
            LessonParticipantRepository participantRepository,
            GroupInvitationRepository invitationRepository,
            AvailabilityRepository availabilityRepository,
            UserRepository userRepository,
            CourseRepository courseRepository,
            PaymentRepository paymentRepository,
            WalletService walletService,
            InvoiceService invoiceService,
            TeacherBalanceService teacherBalanceService,
            ProgressRepository progressRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.lessonRepository = lessonRepository;
        this.participantRepository = participantRepository;
        this.invitationRepository = invitationRepository;
        this.availabilityRepository = availabilityRepository;
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.paymentRepository = paymentRepository;
        this.walletService = walletService;
        this.invoiceService = invoiceService;
        this.teacherBalanceService = teacherBalanceService;
        this.progressRepository = progressRepository;
        this.eventPublisher = eventPublisher;
    }

    // ─── CREATE ──────────────────────────────────────────────

    @Transactional
    public GroupLessonResponse createGroupLesson(Long creatorId, BookGroupLessonRequest request) {
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (creator.getRole() != UserRole.STUDENT) {
            throw new IllegalArgumentException("Only students can create group lessons");
        }

        // Read targetGroupSize from request or from availability
        int targetSize;
        if (request.targetGroupSize() != null) {
            targetSize = request.targetGroupSize();
        } else {
            // Look up from the GROUP availability
            LocalDateTime scheduledAt2 = request.scheduledAt();
            Availability groupAvail = availabilityRepository.findGroupAvailabilityForSlot(
                    request.teacherId(),
                    scheduledAt2.getDayOfWeek(),
                    scheduledAt2.toLocalDate(),
                    scheduledAt2.toLocalTime()
            ).orElseThrow(() -> new IllegalArgumentException("No GROUP availability found for this slot"));
            targetSize = groupAvail.getMaxParticipants();
        }
        if (targetSize < 2 || targetSize > 3) {
            throw new IllegalArgumentException("Group size must be 2 or 3");
        }

        User teacher = userRepository.findById(request.teacherId())
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found"));
        if (teacher.getRole() != UserRole.TEACHER) {
            throw new IllegalArgumentException("Selected user is not a teacher");
        }

        int durationMinutes = request.durationMinutes() != null ? request.durationMinutes() : 60;
        LocalDateTime scheduledAt = request.scheduledAt();

        // Validate time
        if (!scheduledAt.plusMinutes(durationMinutes).isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Lesson must end in the future");
        }

        // Check teacher availability (reuse same logic)
        checkTeacherAvailability(teacher.getId(), scheduledAt, durationMinutes);
        // Check student time conflict
        checkStudentTimeConflict(creatorId, scheduledAt, durationMinutes);

        int teacherRate = teacher.getHourlyRateCents();
        int pricePerPerson = GroupPricingCalculator.calculateParticipantPrice(teacherRate, targetSize);
        int commissionPerPerson = GroupPricingCalculator.calculateCommission(pricePerPerson);

        // Create lesson
        Lesson lesson = new Lesson();
        lesson.setStudent(creator); // backward compat: creator is the "student"
        lesson.setTeacher(teacher);
        lesson.setScheduledAt(scheduledAt);
        lesson.setDurationMinutes(durationMinutes);
        lesson.setNotes(request.notes());
        lesson.setStatus(LessonStatus.PENDING);
        lesson.setPriceCents(teacherRate); // teacher's full rate stored for reference
        lesson.setIsFromSubscription(false);
        lesson.setIsGroupLesson(true);
        lesson.setMaxParticipants(targetSize);
        lesson.setGroupStatus("OPEN");

        if (request.courseId() != null) {
            Course course = courseRepository.findById(request.courseId()).orElse(null);
            lesson.setCourse(course);
        }

        Lesson savedLesson = lessonRepository.save(lesson);

        // Create participant entry for creator
        LessonParticipant creatorParticipant = new LessonParticipant();
        creatorParticipant.setLesson(savedLesson);
        creatorParticipant.setStudent(creator);
        creatorParticipant.setRole("CREATOR");
        creatorParticipant.setStatus("ACTIVE");
        creatorParticipant.setPricePaidCents(pricePerPerson);
        creatorParticipant.setCommissionCents(commissionPerPerson);
        participantRepository.save(creatorParticipant);

        // Create invitation
        GroupInvitation invitation = new GroupInvitation();
        invitation.setToken(UUID.randomUUID().toString());
        invitation.setLesson(savedLesson);
        invitation.setCreatedBy(creator);
        invitation.setMaxParticipants(targetSize);
        invitation.setExpiresAt(scheduledAt.minusHours(24)); // expires 24h before lesson
        invitationRepository.save(invitation);

        // Notify teacher
        publishGroupLessonBookedEvent(savedLesson);

        log.info("Group lesson {} created by user {} for {} participants. Price/person: {} cents",
                savedLesson.getId(), creatorId, targetSize, pricePerPerson);

        return GroupLessonResponse.from(savedLesson, invitation.getToken(), pricePerPerson, invitation.getExpiresAt());
    }

    // ─── INVITATION DETAILS (public) ────────────────────────

    public GroupInvitationResponse getInvitationDetails(String token) {
        GroupInvitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invitation not found"));

        Lesson lesson = invitation.getLesson();
        int pricePerPerson = GroupPricingCalculator.calculateParticipantPrice(
                lesson.getPriceCents(), invitation.getMaxParticipants());

        return GroupInvitationResponse.from(invitation, pricePerPerson);
    }

    // ─── JOIN (wallet) ──────────────────────────────────────

    @Transactional
    public GroupLessonResponse joinWithCredit(Long studentId, String token) {
        GroupInvitation invitation = validateAndLockForJoin(studentId, token);
        Lesson lesson = invitation.getLesson();

        int pricePerPerson = GroupPricingCalculator.calculateParticipantPrice(
                lesson.getPriceCents(), invitation.getMaxParticipants());

        // Add participant FIRST (under pessimistic lock from validateAndLockForJoin)
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        LessonParticipant participant = createParticipant(lesson, student, pricePerPerson);

        // Deduct credit AFTER participant is created (if this fails, @Transactional rolls back the participant too)
        walletService.checkAndDeductCredit(studentId, pricePerPerson);

        // Link wallet transaction to lesson
        walletService.linkDeductionToLesson(studentId, lesson, pricePerPerson);

        // Create payment record
        createPaymentRecord(student, lesson.getTeacher(), lesson, pricePerPerson, PaymentType.LESSON_FROM_CREDIT);

        // Generate invoice
        invoiceService.generateInvoicesForCreditPayment(studentId, lesson.getTeacher().getId(), lesson.getId(), pricePerPerson);

        // Update group status
        updateGroupStatus(lesson);

        // Notify existing participants
        notifyParticipantJoined(lesson, student);

        log.info("Student {} joined group lesson {} with credit. Participants: {}/{}",
                studentId, lesson.getId(), lesson.getActiveParticipantCount(), lesson.getMaxParticipants());

        return buildGroupLessonResponse(lesson, invitation);
    }

    /**
     * Join after Stripe payment (no wallet deduction — Stripe already charged).
     */
    @Transactional
    public GroupLessonResponse joinAfterStripePayment(Long studentId, String token) {
        GroupInvitation invitation = validateAndLockForJoin(studentId, token);
        Lesson lesson = invitation.getLesson();

        int pricePerPerson = GroupPricingCalculator.calculateParticipantPrice(
                lesson.getPriceCents(), invitation.getMaxParticipants());

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Add participant (no wallet deduction)
        LessonParticipant participant = createParticipant(lesson, student, pricePerPerson);

        // Create payment record (Stripe payment, not wallet)
        createPaymentRecord(student, lesson.getTeacher(), lesson, pricePerPerson, PaymentType.ONE_TIME_LESSON);

        // Generate invoice
        invoiceService.generateInvoicesForCreditPayment(studentId, lesson.getTeacher().getId(), lesson.getId(), pricePerPerson);

        // Update group status
        updateGroupStatus(lesson);

        // Notify existing participants
        notifyParticipantJoined(lesson, student);

        log.info("Student {} joined group lesson {} after Stripe payment. Participants: {}/{}",
                studentId, lesson.getId(), lesson.getActiveParticipantCount(), lesson.getMaxParticipants());

        return buildGroupLessonResponse(lesson, invitation);
    }

    // ─── AUTO-JOIN (from booking page, no invitation expiration check) ─────

    /**
     * Auto-join an existing open group with wallet credit.
     * Skips invitation expiration check since the student is booking directly.
     */
    @Transactional
    public GroupLessonResponse autoJoinWithCredit(Long studentId, String token) {
        GroupInvitation invitation = validateAndLockForJoin(studentId, token, true);
        Lesson lesson = invitation.getLesson();

        int pricePerPerson = GroupPricingCalculator.calculateParticipantPrice(
                lesson.getPriceCents(), invitation.getMaxParticipants());

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        LessonParticipant participant = createParticipant(lesson, student, pricePerPerson);

        walletService.checkAndDeductCredit(studentId, pricePerPerson);
        walletService.linkDeductionToLesson(studentId, lesson, pricePerPerson);
        createPaymentRecord(student, lesson.getTeacher(), lesson, pricePerPerson, PaymentType.LESSON_FROM_CREDIT);
        invoiceService.generateInvoicesForCreditPayment(studentId, lesson.getTeacher().getId(), lesson.getId(), pricePerPerson);
        updateGroupStatus(lesson);
        notifyParticipantJoined(lesson, student);

        log.info("Student {} auto-joined group lesson {} with credit. Participants: {}/{}",
                studentId, lesson.getId(), lesson.getActiveParticipantCount(), lesson.getMaxParticipants());

        return buildGroupLessonResponse(lesson, invitation);
    }

    /**
     * Auto-join an existing open group after Stripe payment.
     * Skips invitation expiration check since the student is booking directly.
     */
    @Transactional
    public GroupLessonResponse autoJoinAfterStripePayment(Long studentId, String token) {
        GroupInvitation invitation = validateAndLockForJoin(studentId, token, true);
        Lesson lesson = invitation.getLesson();

        int pricePerPerson = GroupPricingCalculator.calculateParticipantPrice(
                lesson.getPriceCents(), invitation.getMaxParticipants());

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        LessonParticipant participant = createParticipant(lesson, student, pricePerPerson);
        createPaymentRecord(student, lesson.getTeacher(), lesson, pricePerPerson, PaymentType.ONE_TIME_LESSON);
        invoiceService.generateInvoicesForCreditPayment(studentId, lesson.getTeacher().getId(), lesson.getId(), pricePerPerson);
        updateGroupStatus(lesson);
        notifyParticipantJoined(lesson, student);

        log.info("Student {} auto-joined group lesson {} after Stripe payment. Participants: {}/{}",
                studentId, lesson.getId(), lesson.getActiveParticipantCount(), lesson.getMaxParticipants());

        return buildGroupLessonResponse(lesson, invitation);
    }

    // ─── CANCEL PARTICIPANT ─────────────────────────────────

    @Transactional
    public void cancelParticipant(Long lessonId, Long studentId, String reason) {
        Lesson lesson = lessonRepository.findByIdForUpdate(lessonId)
                .orElseThrow(() -> new IllegalArgumentException("Lesson not found"));

        if (!Boolean.TRUE.equals(lesson.getIsGroupLesson())) {
            throw new IllegalArgumentException("Not a group lesson");
        }

        LessonParticipant participant = participantRepository.findActiveByLessonIdAndStudentId(lessonId, studentId)
                .orElseThrow(() -> new IllegalArgumentException("You are not a participant of this lesson"));

        // Block cancellation during the lesson
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lessonEnd = lesson.getScheduledAt().plusMinutes(lesson.getDurationMinutes());
        if (!now.isBefore(lesson.getScheduledAt()) && now.isBefore(lessonEnd)) {
            throw new IllegalArgumentException("errors.cannotCancelDuringLesson");
        }

        // Calculate refund
        int refundPercentage = calculateRefundPercentage(lesson, "STUDENT");
        int refundAmount = (participant.getPricePaidCents() * refundPercentage) / 100;

        // Update participant
        participant.setStatus("CANCELLED");
        participant.setCancelledBy("STUDENT");
        participant.setCancelledAt(LocalDateTime.now());
        participant.setRefundPercentage(refundPercentage);
        participant.setRefundedAmountCents(refundAmount);
        participant.setCancellationReason(reason);
        participantRepository.save(participant);

        // Process refund
        if (refundPercentage > 0) {
            walletService.refundCreditForLesson(studentId, lesson, participant.getPricePaidCents(), refundPercentage);

            // Update payment
            Payment payment = paymentRepository.findByLessonIdAndPayerId(lesson.getId(), studentId).orElse(null);
            if (payment != null) {
                payment.setStatus(PaymentStatus.REFUNDED);
                payment.setRefundReason("Participant left group lesson - " + refundPercentage + "% refund");
                paymentRepository.save(payment);
            }

            invoiceService.generateCreditNoteForWalletRefund(lesson, refundPercentage, refundAmount);
        }

        // Reopen group if was full
        if ("FULL".equals(lesson.getGroupStatus())) {
            lesson.setGroupStatus("OPEN");
        }

        // If no more active participants and lesson hasn't started yet, cancel it
        // If the lesson is already CONFIRMED (in progress), let the teacher complete it
        int remaining = participantRepository.countByLessonIdAndStatus(lessonId, "ACTIVE");
        if (remaining == 0 && lesson.getStatus() == LessonStatus.PENDING) {
            lesson.setStatus(LessonStatus.CANCELLED);
            lesson.setCancelledBy("SYSTEM");
            lesson.setCancelledAt(LocalDateTime.now());
            lesson.setCancellationReason("Groupe annulé automatiquement - aucun participant restant");
        }

        lessonRepository.save(lesson);

        log.info("Participant {} cancelled from group lesson {}. Refund: {}% ({} cents). Remaining: {}",
                studentId, lessonId, refundPercentage, refundAmount, remaining);
    }

    // ─── CANCEL ALL (teacher cancels) ───────────────────────

    @Transactional
    public void cancelGroupByTeacher(Lesson lesson, String reason) {
        List<LessonParticipant> activeParticipants = participantRepository.findByLessonIdAndStatus(lesson.getId(), "ACTIVE");

        for (LessonParticipant participant : activeParticipants) {
            participant.setStatus("CANCELLED");
            participant.setCancelledBy("TEACHER");
            participant.setCancelledAt(LocalDateTime.now());
            participant.setRefundPercentage(100);
            int refundAmount = participant.getPricePaidCents();
            participant.setRefundedAmountCents(refundAmount);
            participant.setCancellationReason(reason);
            participantRepository.save(participant);

            // Refund each participant 100%
            walletService.refundCreditForLesson(
                    participant.getStudent().getId(), lesson, participant.getPricePaidCents(), 100);

            // Update payment
            Payment payment = paymentRepository.findByLessonIdAndPayerId(lesson.getId(), participant.getStudent().getId()).orElse(null);
            if (payment != null) {
                payment.setStatus(PaymentStatus.REFUNDED);
                payment.setRefundReason("Teacher cancelled group lesson - 100% refund");
                paymentRepository.save(payment);
            }
        }

        log.info("Group lesson {} cancelled by teacher. {} participants refunded.", lesson.getId(), activeParticipants.size());
    }

    // ─── DEADLINE HANDLING ──────────────────────────────────

    @Scheduled(cron = "0 */15 * * * *") // every 15 minutes
    @Transactional
    public void checkGroupDeadlines() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = now.plusHours(24);

        List<Lesson> approachingDeadline = lessonRepository.findGroupLessonsApproachingDeadline(now, deadline);

        for (Lesson lesson : approachingDeadline) {
            if (lesson.isGroupFull()) continue; // already full, no action needed

            lesson.setGroupStatus("DEADLINE_PASSED");
            lessonRepository.save(lesson);

            // Notify the creator
            LessonParticipant creator = participantRepository.findByLessonIdAndStatus(lesson.getId(), "ACTIVE")
                    .stream()
                    .filter(p -> "CREATOR".equals(p.getRole()))
                    .findFirst()
                    .orElse(null);

            if (creator != null) {
                notifyDeadlinePassed(lesson, creator.getStudent());
            }

            log.info("Group lesson {} deadline passed. Group has {}/{} participants.",
                    lesson.getId(), lesson.getActiveParticipantCount(), lesson.getMaxParticipants());
        }
    }

    @Transactional
    public void resolveDeadline(Long lessonId, Long creatorId, String choice) {
        Lesson lesson = lessonRepository.findByIdForUpdate(lessonId)
                .orElseThrow(() -> new IllegalArgumentException("Lesson not found"));

        if (!Boolean.TRUE.equals(lesson.getIsGroupLesson())) {
            throw new IllegalArgumentException("Not a group lesson");
        }

        if (!"DEADLINE_PASSED".equals(lesson.getGroupStatus())) {
            throw new IllegalArgumentException("Deadline has not passed yet for this group lesson");
        }

        // Verify the requester is the creator
        LessonParticipant creator = participantRepository.findActiveByLessonIdAndStudentId(lessonId, creatorId)
                .orElseThrow(() -> new IllegalArgumentException("You are not a participant"));
        if (!"CREATOR".equals(creator.getRole())) {
            throw new IllegalArgumentException("Only the creator can resolve the deadline");
        }

        if ("CANCEL".equals(choice)) {
            // Cancel and refund everyone 100%
            cancelGroupBySystem(lesson, "Group not complete before deadline - cancelled by creator");
            lesson.setStatus(LessonStatus.CANCELLED);
            lesson.setCancelledBy("SYSTEM");
            lesson.setCancelledAt(LocalDateTime.now());
            lesson.setCancellationReason("Groupe incomplet - annulé par le créateur");
            lessonRepository.save(lesson);

            log.info("Group lesson {} cancelled by creator at deadline.", lessonId);

        } else if ("PAY_FULL".equals(choice)) {
            // Creator pays the difference to convert to private lesson
            int teacherRate = lesson.getPriceCents();
            int alreadyPaid = creator.getPricePaidCents();
            int difference = teacherRate - alreadyPaid;

            if (difference > 0) {
                walletService.checkAndDeductCredit(creatorId, difference);
                walletService.linkDeductionToLesson(creatorId, lesson, difference);

                // Update creator's paid amount
                creator.setPricePaidCents(teacherRate);
                creator.setCommissionCents(GroupPricingCalculator.calculateCommission(teacherRate));
                participantRepository.save(creator);
            }

            // Cancel other participants and refund them 100%
            List<LessonParticipant> others = participantRepository.findByLessonIdAndStatus(lessonId, "ACTIVE")
                    .stream()
                    .filter(p -> !"CREATOR".equals(p.getRole()))
                    .toList();

            for (LessonParticipant other : others) {
                other.setStatus("CANCELLED");
                other.setCancelledBy("SYSTEM");
                other.setCancelledAt(LocalDateTime.now());
                other.setRefundPercentage(100);
                other.setRefundedAmountCents(other.getPricePaidCents());
                other.setCancellationReason("Group converted to private lesson");
                participantRepository.save(other);

                walletService.refundCreditForLesson(
                        other.getStudent().getId(), lesson, other.getPricePaidCents(), 100);
            }

            // Convert to private lesson
            lesson.setIsGroupLesson(false);
            lesson.setMaxParticipants(1);
            lesson.setGroupStatus(null);
            lessonRepository.save(lesson);

            log.info("Group lesson {} converted to private. Creator paid {} cents difference.", lessonId, difference);

        } else {
            throw new IllegalArgumentException("Invalid choice. Must be 'PAY_FULL' or 'CANCEL'");
        }
    }

    // ─── COMPLETE GROUP LESSON ──────────────────────────────

    @Transactional
    public void handleGroupLessonCompletion(Lesson lesson) {
        List<LessonParticipant> activeParticipants = participantRepository.findByLessonIdAndStatus(lesson.getId(), "ACTIVE");

        // Calculate total collected and teacher earnings
        int totalCollected = activeParticipants.stream()
                .mapToInt(LessonParticipant::getPricePaidCents)
                .sum();
        int totalCommission = GroupPricingCalculator.calculateCommission(totalCollected);
        int teacherEarnings = totalCollected - totalCommission;

        // Override lesson financial fields for group totals
        // Note: priceCents stays as teacher rate. Commission/earnings reflect group totals.
        lesson.setCommissionCents(totalCommission);
        lesson.setTeacherEarningsCents(teacherEarnings);

        // Credit teacher
        if (!Boolean.TRUE.equals(lesson.getEarningsCredited())) {
            teacherBalanceService.creditEarningsForCompletedLesson(lesson);
            lesson.setEarningsCredited(true);
        }

        // Progress for each participant
        for (LessonParticipant participant : activeParticipants) {
            try {
                Progress progress = progressRepository.findByStudentId(participant.getStudent().getId()).orElse(null);
                if (progress != null) {
                    progress.recordCompletedLesson();
                    progressRepository.save(progress);
                }
            } catch (Exception e) {
                log.warn("Could not update progress for participant {}: {}", participant.getStudent().getId(), e.getMessage());
            }
        }

        lessonRepository.save(lesson);

        log.info("Group lesson {} completed. {} participants. Total: {} cents, Teacher earnings: {} cents",
                lesson.getId(), activeParticipants.size(), totalCollected, teacherEarnings);
    }

    // ─── GET DETAILS ────────────────────────────────────────

    public GroupLessonResponse getGroupLessonDetails(Long lessonId, Long userId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new IllegalArgumentException("Lesson not found"));

        if (!Boolean.TRUE.equals(lesson.getIsGroupLesson())) {
            throw new IllegalArgumentException("Not a group lesson");
        }

        // Check access: must be teacher, admin, or participant
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        boolean isTeacher = lesson.getTeacher().getId().equals(userId);
        boolean isParticipant = participantRepository.existsActiveByLessonIdAndStudentId(lessonId, userId);
        boolean isAdmin = user.getRole() == UserRole.ADMIN;

        if (!isTeacher && !isParticipant && !isAdmin) {
            throw new IllegalArgumentException("Not authorized to view this lesson");
        }

        GroupInvitation invitation = invitationRepository.findByLessonId(lessonId).orElse(null);
        String token = invitation != null ? invitation.getToken() : null;
        LocalDateTime deadline = invitation != null ? invitation.getExpiresAt() : null;
        int pricePerPerson = GroupPricingCalculator.calculateParticipantPrice(
                lesson.getPriceCents(), lesson.getMaxParticipants());

        return GroupLessonResponse.from(lesson, token, pricePerPerson, deadline);
    }

    // ─── HELPERS ────────────────────────────────────────────

    private GroupInvitation validateAndLockForJoin(Long studentId, String token) {
        return validateAndLockForJoin(studentId, token, false);
    }

    private GroupInvitation validateAndLockForJoin(Long studentId, String token, boolean skipExpirationCheck) {
        GroupInvitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invitation not found"));

        if (!skipExpirationCheck && invitation.isExpired()) {
            throw new IllegalArgumentException("This invitation has expired");
        }

        // Lock the lesson row to prevent race condition
        Lesson lesson = lessonRepository.findByIdForUpdate(invitation.getLesson().getId())
                .orElseThrow(() -> new IllegalArgumentException("Lesson not found"));

        if (lesson.getStatus() == LessonStatus.CANCELLED) {
            throw new IllegalArgumentException("This lesson has been cancelled");
        }

        // Check "already a participant" BEFORE "full" to handle double-confirmation gracefully
        if (participantRepository.existsByLessonIdAndStudentId(lesson.getId(), studentId)) {
            throw new IllegalArgumentException("You are already a participant");
        }

        if (lesson.isGroupFull()) {
            throw new IllegalArgumentException("This group lesson is already full");
        }

        // Check the joining student is actually a student
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (student.getRole() != UserRole.STUDENT) {
            throw new IllegalArgumentException("Only students can join group lessons");
        }

        // Cannot be the teacher
        if (lesson.getTeacher().getId().equals(studentId)) {
            throw new IllegalArgumentException("The teacher cannot join as a participant");
        }

        // Check time conflict
        int duration = lesson.getDurationMinutes();
        checkStudentTimeConflict(studentId, lesson.getScheduledAt(), duration);

        return invitation;
    }

    private LessonParticipant createParticipant(Lesson lesson, User student, int pricePerPerson) {
        int commission = GroupPricingCalculator.calculateCommission(pricePerPerson);

        LessonParticipant participant = new LessonParticipant();
        participant.setLesson(lesson);
        participant.setStudent(student);
        participant.setRole("PARTICIPANT");
        participant.setStatus("ACTIVE");
        participant.setPricePaidCents(pricePerPerson);
        participant.setCommissionCents(commission);
        return participantRepository.save(participant);
    }

    private void createPaymentRecord(User student, User teacher, Lesson lesson, int amount, PaymentType type) {
        Payment payment = new Payment();
        payment.setPayer(student);
        payment.setTeacher(teacher);
        payment.setLesson(lesson);
        payment.setPaymentType(type);
        payment.setAmountCents(amount);
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setProcessedAt(LocalDateTime.now());
        paymentRepository.save(payment);
    }

    private void updateGroupStatus(Lesson lesson) {
        // Refresh participant count
        int count = participantRepository.countByLessonIdAndStatus(lesson.getId(), "ACTIVE");
        if (count >= lesson.getMaxParticipants()) {
            lesson.setGroupStatus("FULL");
            lessonRepository.save(lesson);
        }
    }

    private void cancelGroupBySystem(Lesson lesson, String reason) {
        List<LessonParticipant> activeParticipants = participantRepository.findByLessonIdAndStatus(lesson.getId(), "ACTIVE");

        for (LessonParticipant participant : activeParticipants) {
            participant.setStatus("CANCELLED");
            participant.setCancelledBy("SYSTEM");
            participant.setCancelledAt(LocalDateTime.now());
            participant.setRefundPercentage(100);
            participant.setRefundedAmountCents(participant.getPricePaidCents());
            participant.setCancellationReason(reason);
            participantRepository.save(participant);

            walletService.refundCreditForLesson(
                    participant.getStudent().getId(), lesson, participant.getPricePaidCents(), 100);

            Payment payment = paymentRepository.findByLessonIdAndPayerId(lesson.getId(), participant.getStudent().getId()).orElse(null);
            if (payment != null) {
                payment.setStatus(PaymentStatus.REFUNDED);
                payment.setRefundReason(reason);
                paymentRepository.save(payment);
            }
        }
    }

    private void checkTeacherAvailability(Long teacherId, LocalDateTime scheduledAt, int durationMinutes) {
        LocalDateTime bufferStart = scheduledAt.minusMinutes(30);
        LocalDateTime bufferEnd = scheduledAt.plusMinutes(durationMinutes + 30);

        List<Lesson> conflictingLessons = lessonRepository.findTeacherLessonsBetween(teacherId, bufferStart, bufferEnd);
        boolean hasConflict = conflictingLessons.stream()
                .anyMatch(l -> l.getStatus() == LessonStatus.PENDING || l.getStatus() == LessonStatus.CONFIRMED);

        if (hasConflict) {
            throw new IllegalArgumentException("errors.teacherNotAvailable");
        }
    }

    private void checkStudentTimeConflict(Long studentId, LocalDateTime scheduledAt, int durationMinutes) {
        LocalDateTime endTime = scheduledAt.plusMinutes(durationMinutes);

        List<Lesson> existingLessons = lessonRepository.findStudentLessonsBetween(
                studentId, scheduledAt.minusMinutes(durationMinutes - 1), endTime);

        boolean hasConflict = existingLessons.stream()
                .filter(l -> l.getStatus() == LessonStatus.PENDING || l.getStatus() == LessonStatus.CONFIRMED)
                .anyMatch(existing -> {
                    LocalDateTime existingEnd = existing.getScheduledAt().plusMinutes(existing.getDurationMinutes());
                    return scheduledAt.isBefore(existingEnd) && endTime.isAfter(existing.getScheduledAt());
                });

        if (hasConflict) {
            throw new IllegalArgumentException("errors.timeConflict");
        }
    }

    private int calculateRefundPercentage(Lesson lesson, String cancelledBy) {
        if ("TEACHER".equals(cancelledBy) || "SYSTEM".equals(cancelledBy)) {
            return 100;
        }
        long hoursUntilLesson = ChronoUnit.HOURS.between(LocalDateTime.now(), lesson.getScheduledAt());
        if (hoursUntilLesson >= FULL_REFUND_HOURS) return 100;
        if (hoursUntilLesson >= PARTIAL_REFUND_HOURS) return 50;
        return 0;
    }

    private GroupLessonResponse buildGroupLessonResponse(Lesson lesson, GroupInvitation invitation) {
        int pricePerPerson = GroupPricingCalculator.calculateParticipantPrice(
                lesson.getPriceCents(), lesson.getMaxParticipants());
        return GroupLessonResponse.from(lesson, invitation.getToken(), pricePerPerson, invitation.getExpiresAt());
    }

    // ─── NOTIFICATIONS ──────────────────────────────────────

    private void publishGroupLessonBookedEvent(Lesson lesson) {
        try {
            LessonStatusPayload payload = new LessonStatusPayload(
                    lesson.getId(), null, LessonStatus.PENDING.name(),
                    lesson.getTeacher().getDisplayName(),
                    lesson.getStudent().getDisplayName(),
                    lesson.getScheduledAt().format(DATE_FORMATTER)
            );
            eventPublisher.publishEvent(new NotificationEvent(
                    this, NotificationEvent.EventType.LESSON_BOOKED,
                    lesson.getTeacher().getId(), payload));
        } catch (Exception e) {
            log.warn("Failed to publish group lesson booked event: {}", e.getMessage());
        }
    }

    private void notifyParticipantJoined(Lesson lesson, User newParticipant) {
        try {
            List<LessonParticipant> participants = participantRepository.findByLessonIdAndStatus(lesson.getId(), "ACTIVE");
            for (LessonParticipant p : participants) {
                if (!p.getStudent().getId().equals(newParticipant.getId())) {
                    LessonStatusPayload payload = new LessonStatusPayload(
                            lesson.getId(), null, "PARTICIPANT_JOINED",
                            lesson.getTeacher().getDisplayName(),
                            newParticipant.getDisplayName(),
                            lesson.getScheduledAt().format(DATE_FORMATTER)
                    );
                    eventPublisher.publishEvent(new NotificationEvent(
                            this, NotificationEvent.EventType.LESSON_STATUS_CHANGED,
                            p.getStudent().getId(), payload));
                }
            }
            // Also notify teacher
            LessonStatusPayload teacherPayload = new LessonStatusPayload(
                    lesson.getId(), null, "PARTICIPANT_JOINED",
                    lesson.getTeacher().getDisplayName(),
                    newParticipant.getDisplayName(),
                    lesson.getScheduledAt().format(DATE_FORMATTER)
            );
            eventPublisher.publishEvent(new NotificationEvent(
                    this, NotificationEvent.EventType.LESSON_STATUS_CHANGED,
                    lesson.getTeacher().getId(), teacherPayload));
        } catch (Exception e) {
            log.warn("Failed to notify participant joined: {}", e.getMessage());
        }
    }

    private void notifyDeadlinePassed(Lesson lesson, User creator) {
        try {
            LessonStatusPayload payload = new LessonStatusPayload(
                    lesson.getId(), "OPEN", "DEADLINE_PASSED",
                    lesson.getTeacher().getDisplayName(),
                    creator.getDisplayName(),
                    lesson.getScheduledAt().format(DATE_FORMATTER)
            );
            eventPublisher.publishEvent(new NotificationEvent(
                    this, NotificationEvent.EventType.LESSON_STATUS_CHANGED,
                    creator.getId(), payload));
        } catch (Exception e) {
            log.warn("Failed to notify deadline passed: {}", e.getMessage());
        }
    }
}
