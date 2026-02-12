package com.chessconnect.service;

import com.chessconnect.dto.admin.*;
import com.chessconnect.dto.lesson.LessonResponse;
import com.chessconnect.model.Invoice;
import com.chessconnect.model.Lesson;
import com.chessconnect.model.Payment;
import com.chessconnect.model.Subscription;
import com.chessconnect.model.TeacherBalance;
import com.chessconnect.model.User;
import com.chessconnect.model.enums.InvoiceType;
import com.chessconnect.model.enums.LessonStatus;
import com.chessconnect.model.enums.PaymentStatus;
import com.chessconnect.model.enums.UserRole;
import com.chessconnect.model.TeacherPayout;
import com.chessconnect.repository.*;
import com.chessconnect.repository.EmailVerificationTokenRepository;
import com.stripe.model.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);

    private final UserRepository userRepository;
    private final LessonRepository lessonRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;
    private final TeacherBalanceRepository teacherBalanceRepository;
    private final RatingRepository ratingRepository;
    private final TeacherPayoutRepository teacherPayoutRepository;
    private final StripeConnectService stripeConnectService;
    private final AvailabilityRepository availabilityRepository;
    private final FavoriteTeacherRepository favoriteTeacherRepository;
    private final QuizResultRepository quizResultRepository;
    private final UserCourseProgressRepository userCourseProgressRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final ProgressRepository progressRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceService invoiceService;
    private final WalletService walletService;
    private final UserNotificationService userNotificationService;
    private final UserNotificationRepository userNotificationRepository;
    private final CreditTransactionRepository creditTransactionRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PageViewRepository pageViewRepository;
    private final StudentWalletRepository studentWalletRepository;
    private final LessonParticipantRepository lessonParticipantRepository;
    private final GroupInvitationRepository groupInvitationRepository;
    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final VideoWatchProgressRepository videoWatchProgressRepository;
    private final PendingCourseValidationRepository pendingCourseValidationRepository;

    public AdminService(
            UserRepository userRepository,
            LessonRepository lessonRepository,
            SubscriptionRepository subscriptionRepository,
            PaymentRepository paymentRepository,
            TeacherBalanceRepository teacherBalanceRepository,
            RatingRepository ratingRepository,
            TeacherPayoutRepository teacherPayoutRepository,
            StripeConnectService stripeConnectService,
            AvailabilityRepository availabilityRepository,
            FavoriteTeacherRepository favoriteTeacherRepository,
            QuizResultRepository quizResultRepository,
            UserCourseProgressRepository userCourseProgressRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            ProgressRepository progressRepository,
            InvoiceRepository invoiceRepository,
            InvoiceService invoiceService,
            WalletService walletService,
            UserNotificationService userNotificationService,
            UserNotificationRepository userNotificationRepository,
            CreditTransactionRepository creditTransactionRepository,
            EmailVerificationTokenRepository emailVerificationTokenRepository,
            PageViewRepository pageViewRepository,
            StudentWalletRepository studentWalletRepository,
            LessonParticipantRepository lessonParticipantRepository,
            GroupInvitationRepository groupInvitationRepository,
            PushSubscriptionRepository pushSubscriptionRepository,
            VideoWatchProgressRepository videoWatchProgressRepository,
            PendingCourseValidationRepository pendingCourseValidationRepository
    ) {
        this.userRepository = userRepository;
        this.lessonRepository = lessonRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.paymentRepository = paymentRepository;
        this.teacherBalanceRepository = teacherBalanceRepository;
        this.ratingRepository = ratingRepository;
        this.teacherPayoutRepository = teacherPayoutRepository;
        this.stripeConnectService = stripeConnectService;
        this.availabilityRepository = availabilityRepository;
        this.favoriteTeacherRepository = favoriteTeacherRepository;
        this.quizResultRepository = quizResultRepository;
        this.userCourseProgressRepository = userCourseProgressRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.progressRepository = progressRepository;
        this.invoiceRepository = invoiceRepository;
        this.invoiceService = invoiceService;
        this.walletService = walletService;
        this.userNotificationService = userNotificationService;
        this.userNotificationRepository = userNotificationRepository;
        this.creditTransactionRepository = creditTransactionRepository;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.pageViewRepository = pageViewRepository;
        this.studentWalletRepository = studentWalletRepository;
        this.lessonParticipantRepository = lessonParticipantRepository;
        this.groupInvitationRepository = groupInvitationRepository;
        this.pushSubscriptionRepository = pushSubscriptionRepository;
        this.videoWatchProgressRepository = videoWatchProgressRepository;
        this.pendingCourseValidationRepository = pendingCourseValidationRepository;
    }

    /**
     * Get all users with pagination and optional role filter
     * Note: ADMIN users are excluded from the list
     * Optimized: Uses batch queries to avoid N+1 problem
     */
    @Transactional(readOnly = true)
    public Page<UserListResponse> getUsers(Pageable pageable, String roleFilter) {
        Page<User> users;
        if (roleFilter != null && !roleFilter.isBlank()) {
            UserRole role = UserRole.valueOf(roleFilter.toUpperCase());
            // Don't allow filtering by ADMIN role
            if (role == UserRole.ADMIN) {
                return Page.empty(pageable);
            }
            users = userRepository.findByRole(role, pageable);
        } else {
            // Exclude ADMIN users from the list
            users = userRepository.findByRoleNot(UserRole.ADMIN, pageable);
        }

        // Batch load lesson counts for all users on this page
        List<Long> userIds = users.getContent().stream().map(User::getId).toList();
        Map<Long, Long> lessonCountsMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            lessonRepository.countLessonsByUserIds(userIds).forEach(row -> {
                Long userId = (Long) row[0];
                Long count = (Long) row[1];
                lessonCountsMap.put(userId, count);
            });
        }

        // Batch load ratings for teachers on this page
        List<Long> teacherIds = users.getContent().stream()
                .filter(u -> u.getRole() == UserRole.TEACHER)
                .map(User::getId)
                .toList();
        Map<Long, Double> avgRatingsMap = new HashMap<>();
        Map<Long, Long> reviewCountsMap = new HashMap<>();
        if (!teacherIds.isEmpty()) {
            ratingRepository.getRatingsStatsByTeacherIds(teacherIds).forEach(row -> {
                Long teacherId = (Long) row[0];
                Double avgRating = (Double) row[1];
                Long reviewCount = (Long) row[2];
                avgRatingsMap.put(teacherId, avgRating);
                reviewCountsMap.put(teacherId, reviewCount);
            });
        }

        return users.map(user -> {
            Long lessonsCount = lessonCountsMap.getOrDefault(user.getId(), 0L);
            Double avgRating = null;
            Long reviewCount = null;
            if (user.getRole() == UserRole.TEACHER) {
                avgRating = avgRatingsMap.get(user.getId());
                reviewCount = reviewCountsMap.getOrDefault(user.getId(), 0L);
            }
            return UserListResponse.from(user, lessonsCount, avgRating, reviewCount);
        });
    }

    /**
     * Get user details by ID
     */
    @Transactional(readOnly = true)
    public UserListResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Long lessonsCount = lessonRepository.countByStudentIdOrTeacherId(user.getId());
        Double avgRating = null;
        Long reviewCount = null;
        if (user.getRole() == UserRole.TEACHER) {
            avgRating = ratingRepository.getAverageRatingForTeacher(user.getId());
            Integer count = ratingRepository.getReviewCountForTeacher(user.getId());
            reviewCount = count != null ? count.longValue() : 0L;
        }
        return UserListResponse.from(user, lessonsCount, avgRating, reviewCount);
    }

    /**
     * Suspend a user
     */
    @Transactional
    public void suspendUser(Long userId, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setIsSuspended(true);
        userRepository.save(user);
        log.info("User {} suspended. Reason: {}", userId, reason);
    }

    /**
     * Reactivate a user
     */
    @Transactional
    public void activateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setIsSuspended(false);
        userRepository.save(user);
        log.info("User {} reactivated", userId);
    }

    /**
     * Delete a user permanently.
     * For teachers: automatically cancels all pending/confirmed lessons and refunds students.
     * Deletes all related data in correct order to avoid FK constraint violations.
     */
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Prevent deleting admin users
        if (user.getRole() == UserRole.ADMIN) {
            throw new IllegalArgumentException("Cannot delete admin users");
        }

        log.info("Deleting user {} ({} {}) and all related data...", userId, user.getFirstName(), user.getLastName());

        // For teachers: cancel all active lessons and refund students
        if (user.getRole() == UserRole.TEACHER) {
            cancelAndRefundTeacherLessons(userId, user.getFullName());
        }

        // For students: cancel their active lessons (student-initiated cancellation rules apply)
        if (user.getRole() == UserRole.STUDENT) {
            cancelStudentLessons(userId);
        }

        // Delete all related data in correct order (FK dependencies)
        // 1. Password reset tokens and email verification tokens
        passwordResetTokenRepository.deleteByUserId(userId);
        emailVerificationTokenRepository.deleteByUserId(userId);
        log.debug("Deleted password reset and email verification tokens for user {}", userId);

        // 2. Ratings - delete ratings that reference lessons belonging to this user first (FK constraint)
        ratingRepository.deleteByLessonUserId(userId);
        log.debug("Deleted ratings by lesson for user {}", userId);

        // 3. Ratings (as student who gave ratings, or as teacher who received ratings)
        ratingRepository.deleteByStudentId(userId);
        ratingRepository.deleteByTeacherId(userId);
        log.debug("Deleted ratings for user {}", userId);

        // 3. Favorite teachers (as student or as teacher)
        favoriteTeacherRepository.deleteByStudentId(userId);
        favoriteTeacherRepository.deleteByTeacherId(userId);
        log.debug("Deleted favorite teachers for user {}", userId);

        // 4. Quiz results
        quizResultRepository.deleteByStudentId(userId);
        log.debug("Deleted quiz results for user {}", userId);

        // 5. User course progress
        userCourseProgressRepository.deleteByUserId(userId);
        log.debug("Deleted user course progress for user {}", userId);

        // 6. Payments (as payer or as teacher receiving payment)
        paymentRepository.deleteByPayerId(userId);
        paymentRepository.deleteByTeacherId(userId);
        log.debug("Deleted payments for user {}", userId);

        // 7. Teacher-specific data (if user is a teacher)
        if (user.getRole() == UserRole.TEACHER) {
            availabilityRepository.deleteByTeacherId(userId);
            log.debug("Deleted availabilities for teacher {}", userId);

            teacherPayoutRepository.deleteByTeacherId(userId);
            log.debug("Deleted teacher payouts for teacher {}", userId);

            teacherBalanceRepository.deleteByTeacherId(userId);
            log.debug("Deleted teacher balance for teacher {}", userId);
        }

        // 8. Invoices - PRESERVE for legal compliance (10 year retention, Code de commerce Art. L123-22)
        // Nullify FK references but keep denormalized name/email fields
        invoiceRepository.nullifyOriginalInvoiceByUserId(userId);
        invoiceRepository.nullifyCustomerId(userId);
        invoiceRepository.nullifyIssuerId(userId);
        log.debug("Nullified user references in invoices for user {} (invoices preserved for legal retention)", userId);

        // 9. Credit transactions (references lessons, must be before lesson deletion)
        creditTransactionRepository.deleteByUserId(userId);
        creditTransactionRepository.deleteByLessonStudentId(userId);
        creditTransactionRepository.deleteByLessonTeacherId(userId);
        log.debug("Deleted credit transactions for user {}", userId);

        // 10. Pending course validations
        pendingCourseValidationRepository.deleteByTeacherId(userId);
        pendingCourseValidationRepository.deleteByStudentId(userId);
        log.debug("Deleted pending course validations for user {}", userId);

        // 11. Lesson participants and group invitations (FK to lessons)
        lessonParticipantRepository.deleteByStudentId(userId);
        groupInvitationRepository.deleteByCreatedById(userId);
        log.debug("Deleted lesson participants and group invitations for user {}", userId);

        // 12. Nullify lesson references in invoices (preserves invoices for accounting)
        invoiceRepository.nullifyLessonByStudentId(userId);
        invoiceRepository.nullifyLessonByTeacherId(userId);
        log.debug("Nullified lesson references in invoices for user {}", userId);

        // 13. Clean up lesson-related FKs (participants, invitations) for ALL lessons of this user
        List<Lesson> userLessons = new java.util.ArrayList<>();
        userLessons.addAll(lessonRepository.findByStudentIdOrderByScheduledAtDesc(userId));
        userLessons.addAll(lessonRepository.findByTeacherIdOrderByScheduledAtDesc(userId));
        for (Lesson lesson : userLessons) {
            lessonParticipantRepository.deleteByLessonId(lesson.getId());
            groupInvitationRepository.deleteByLessonId(lesson.getId());
            pendingCourseValidationRepository.deleteByLessonId(lesson.getId());
        }
        log.debug("Deleted lesson participants/invitations/validations for user {}'s lessons", userId);

        // 14. Lessons (as student or teacher) - must be after all FK references are removed
        lessonRepository.deleteByStudentId(userId);
        lessonRepository.deleteByTeacherId(userId);
        log.debug("Deleted lessons for user {}", userId);

        // 14. Subscriptions
        subscriptionRepository.deleteByStudentId(userId);
        log.debug("Deleted subscriptions for user {}", userId);

        // 15. Progress
        progressRepository.deleteByStudentId(userId);
        log.debug("Deleted progress for user {}", userId);

        // 16. User notifications
        userNotificationRepository.deleteByUserId(userId);
        log.debug("Deleted notifications for user {}", userId);

        // 17. Page views (analytics)
        pageViewRepository.deleteByUserId(userId);
        log.debug("Deleted page views for user {}", userId);

        // 18. Video watch progress
        videoWatchProgressRepository.deleteByUserId(userId);
        log.debug("Deleted video watch progress for user {}", userId);

        // 19. Push subscriptions
        pushSubscriptionRepository.deleteByUserId(userId);
        log.debug("Deleted push subscriptions for user {}", userId);

        // 20. Student wallet
        studentWalletRepository.deleteByUserId(userId);
        log.debug("Deleted student wallet for user {}", userId);

        // 21. Finally delete the user
        userRepository.delete(user);
        log.info("User {} deleted successfully", userId);
    }

    /**
     * Cancel all active lessons for a teacher and refund students 100%.
     * Called when admin deletes a teacher account.
     */
    private void cancelAndRefundTeacherLessons(Long teacherId, String teacherName) {
        List<Lesson> activeLessons = lessonRepository.findByTeacherIdAndStatusIn(
                teacherId,
                List.of(LessonStatus.PENDING, LessonStatus.CONFIRMED)
        );

        if (activeLessons.isEmpty()) {
            log.info("No active lessons to cancel for teacher {}", teacherId);
            return;
        }

        log.info("Cancelling {} active lessons for teacher {} and refunding students...", activeLessons.size(), teacherId);

        for (Lesson lesson : activeLessons) {
            try {
                // Set cancellation info
                lesson.setCancelledBy("ADMIN");
                lesson.setCancellationReason("Compte coach supprime par l'administrateur");
                lesson.setCancelledAt(LocalDateTime.now());
                lesson.setStatus(LessonStatus.CANCELLED);
                lesson.setRefundPercentage(100); // Full refund for admin deletion

                // Process refund to student's wallet
                if (lesson.getPriceCents() != null && lesson.getPriceCents() > 0) {
                    Payment payment = paymentRepository.findByLessonId(lesson.getId()).orElse(null);

                    if (payment != null) {
                        int refundAmount = lesson.getPriceCents();

                        // Refund to student's wallet
                        walletService.refundCreditForLesson(
                                lesson.getStudent().getId(),
                                lesson,
                                lesson.getPriceCents(),
                                100 // 100% refund
                        );

                        lesson.setRefundedAmountCents(refundAmount);
                        payment.setStatus(PaymentStatus.REFUNDED);
                        payment.setRefundReason("Compte coach supprime - remboursement 100%");
                        paymentRepository.save(payment);

                        // Generate credit note
                        invoiceService.generateCreditNoteForWalletRefund(lesson, 100, refundAmount);

                        // Send notification to student about the refund
                        userNotificationService.notifyRefund(
                                lesson.getStudent().getId(),
                                refundAmount,
                                "Suite a la suppression du compte de votre coach " + teacherName + "."
                        );

                        log.info("Refunded {} cents to student {} for lesson {}",
                                refundAmount, lesson.getStudent().getId(), lesson.getId());
                    }
                }

                lessonRepository.save(lesson);
                log.info("Cancelled lesson {} (student: {}, scheduled: {})",
                        lesson.getId(), lesson.getStudent().getEmail(), lesson.getScheduledAt());

            } catch (Exception e) {
                log.error("Failed to cancel/refund lesson {}: {}", lesson.getId(), e.getMessage());
                // Continue with other lessons even if one fails
            }
        }

        log.info("Finished cancelling {} lessons for teacher {}", activeLessons.size(), teacherId);
    }

    /**
     * Cancel all active lessons for a student being deleted.
     * Uses normal student cancellation rules (may not get full refund depending on timing).
     */
    private void cancelStudentLessons(Long studentId) {
        List<Lesson> activeLessons = lessonRepository.findByStudentIdAndStatusIn(
                studentId,
                List.of(LessonStatus.PENDING, LessonStatus.CONFIRMED)
        );

        if (activeLessons.isEmpty()) {
            log.info("No active lessons to cancel for student {}", studentId);
            return;
        }

        log.info("Cancelling {} active lessons for student {}...", activeLessons.size(), studentId);

        for (Lesson lesson : activeLessons) {
            try {
                // Set cancellation info - use ADMIN since admin is deleting
                lesson.setCancelledBy("ADMIN");
                lesson.setCancellationReason("Compte joueur supprime par l'administrateur");
                lesson.setCancelledAt(LocalDateTime.now());
                lesson.setStatus(LessonStatus.CANCELLED);
                lesson.setRefundPercentage(100); // Full refund when admin deletes

                // Process refund - but since student is being deleted, just mark as refunded
                // No need to credit wallet since account is being deleted
                if (lesson.getPriceCents() != null && lesson.getPriceCents() > 0) {
                    Payment payment = paymentRepository.findByLessonId(lesson.getId()).orElse(null);
                    if (payment != null) {
                        lesson.setRefundedAmountCents(lesson.getPriceCents());
                        payment.setStatus(PaymentStatus.REFUNDED);
                        payment.setRefundReason("Compte joueur supprime - pas de remboursement (compte supprime)");
                        paymentRepository.save(payment);
                    }
                }

                lessonRepository.save(lesson);
                log.info("Cancelled lesson {} for deleted student", lesson.getId());

            } catch (Exception e) {
                log.error("Failed to cancel lesson {} for student: {}", lesson.getId(), e.getMessage());
            }
        }
    }

    /**
     * Get upcoming lessons (PENDING or CONFIRMED) - ordered by scheduled date ascending
     * Optimized: Uses database query instead of findAll() + in-memory filtering
     */
    @Transactional(readOnly = true)
    public List<LessonResponse> getUpcomingLessons() {
        List<LessonStatus> statuses = List.of(LessonStatus.PENDING, LessonStatus.CONFIRMED);
        LocalDateTime cutoff = LocalDateTime.now().minusHours(1);
        return lessonRepository.findByStatusInAndScheduledAtAfter(statuses, cutoff).stream()
                .map(LessonResponse::from)
                .toList();
    }

    /**
     * Get completed lessons - ordered by scheduled date descending
     * Optimized: Uses database query instead of findAll() + in-memory filtering
     */
    @Transactional(readOnly = true)
    public List<LessonResponse> getCompletedLessons() {
        return lessonRepository.findByStatusOrderByScheduledAtDesc(LessonStatus.COMPLETED).stream()
                .map(LessonResponse::from)
                .toList();
    }

    /**
     * Get all past lessons (COMPLETED + CANCELLED) - for admin history view
     * Includes cancelled lessons for abuse/complaint investigation
     */
    @Transactional(readOnly = true)
    public List<LessonResponse> getPastLessons() {
        return lessonRepository.findAllPastLessons().stream()
                .map(LessonResponse::from)
                .toList();
    }

    /**
     * Get ALL lessons (all statuses) - for complete admin overview
     */
    @Transactional(readOnly = true)
    public List<LessonResponse> getAllLessons() {
        return lessonRepository.findAllOrderByScheduledAtDesc().stream()
                .map(LessonResponse::from)
                .toList();
    }

    /**
     * Get accounting/revenue overview
     * Optimized: Uses aggregate queries instead of loading all lessons into memory
     */
    @Transactional(readOnly = true)
    public AccountingResponse getAccountingOverview() {
        // Use aggregate queries - much faster than loading all lessons
        long totalRevenue = lessonRepository.sumPriceCentsCompleted();
        long totalCommissions = lessonRepository.sumCommissionCentsCompleted();
        long totalTeacherEarnings = lessonRepository.sumTeacherEarningsCentsCompleted();
        long totalRefunded = lessonRepository.sumRefundedAmountCentsCancelled();
        long totalLessons = lessonRepository.count();
        long completedLessons = lessonRepository.countCompleted();
        long cancelledLessons = lessonRepository.countCancelled();

        return new AccountingResponse(
                totalRevenue,
                totalCommissions,
                totalTeacherEarnings,
                totalRefunded,
                totalLessons,
                completedLessons,
                cancelledLessons
        );
    }

    /**
     * Get all teacher balances with banking info and current month payout status
     * Optimized: Uses specific queries per teacher instead of loading all lessons
     */
    @Transactional(readOnly = true)
    public List<TeacherBalanceListResponse> getTeacherBalances() {
        List<TeacherBalance> balances = teacherBalanceRepository.findAll();
        String currentYearMonth = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

        // Get current month date range
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime monthStart = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = currentMonth.atEndOfMonth().atTime(23, 59, 59);

        return balances.stream().map(balance -> {
            User teacher = balance.getTeacher();

            // Use optimized queries instead of loading all lessons
            Integer currentMonthEarnings = lessonRepository.sumTeacherEarningsBetween(teacher.getId(), monthStart, monthEnd);
            Integer currentMonthLessonsCount = lessonRepository.countTeacherCompletedBetween(teacher.getId(), monthStart, monthEnd);

            // Check if paid for current month
            TeacherPayout payout = teacherPayoutRepository
                    .findByTeacherIdAndYearMonth(teacher.getId(), currentYearMonth)
                    .orElse(null);

            // Check Stripe Connect status
            boolean stripeConnectEnabled = teacher.getStripeConnectAccountId() != null &&
                    !teacher.getStripeConnectAccountId().isBlank();
            boolean stripeConnectReady = stripeConnectEnabled &&
                    Boolean.TRUE.equals(teacher.getStripeConnectOnboardingComplete());

            return new TeacherBalanceListResponse(
                    teacher.getId(),
                    teacher.getFirstName(),
                    teacher.getLastName(),
                    teacher.getEmail(),
                    balance.getAvailableBalanceCents(),
                    balance.getPendingBalanceCents(),
                    balance.getTotalEarnedCents(),
                    balance.getTotalWithdrawnCents(),
                    balance.getLessonsCompleted(),
                    // Banking info masked for security
                    TeacherBalanceListResponse.maskIban(teacher.getIban()),
                    maskBic(teacher.getBic()),
                    teacher.getAccountHolderName(),
                    maskSiret(teacher.getSiret()),
                    teacher.getCompanyName(),
                    // Current month payout status
                    payout != null && payout.getIsPaid(),
                    currentMonthEarnings != null ? currentMonthEarnings : 0,
                    currentMonthLessonsCount != null ? currentMonthLessonsCount : 0,
                    // Stripe Connect status
                    stripeConnectEnabled,
                    stripeConnectReady
            );
        }).toList();
    }

    /**
     * Mark a teacher as paid - transfers a custom amount or the full available balance.
     * This performs a real Stripe Connect transfer to the teacher's connected account.
     */
    @Transactional
    public TeacherPayoutResult markTeacherPaid(Long teacherId, String yearMonth, String paymentReference, String notes, Integer customAmountCents) {
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new IllegalArgumentException("Coach non trouve"));

        // Verify teacher has Stripe Connect set up
        if (teacher.getStripeConnectAccountId() == null || teacher.getStripeConnectAccountId().isBlank()) {
            throw new IllegalArgumentException("Le coach n'a pas configure son compte Stripe Connect");
        }

        // Get teacher's balance
        TeacherBalance balance = teacherBalanceRepository.findByTeacherId(teacherId)
                .orElseThrow(() -> new IllegalArgumentException("Aucun solde trouve pour ce coach"));

        int availableBalance = balance.getAvailableBalanceCents() != null ? balance.getAvailableBalanceCents() : 0;

        if (availableBalance <= 0) {
            throw new IllegalArgumentException("Aucun solde disponible a transferer");
        }

        // Use custom amount if provided, otherwise use full available balance
        int transferAmount = customAmountCents != null && customAmountCents > 0 ? customAmountCents : availableBalance;

        // Validate transfer amount doesn't exceed available balance
        if (transferAmount > availableBalance) {
            throw new IllegalArgumentException("Le montant demande (" + (transferAmount / 100.0) + " EUR) depasse le solde disponible (" + (availableBalance / 100.0) + " EUR)");
        }

        // Get or create payout record for tracking
        TeacherPayout payout = teacherPayoutRepository
                .findByTeacherIdAndYearMonth(teacherId, yearMonth)
                .orElseGet(() -> {
                    TeacherPayout newPayout = new TeacherPayout();
                    newPayout.setTeacher(teacher);
                    newPayout.setYearMonth(yearMonth);
                    return newPayout;
                });

        // Perform the Stripe transfer
        String stripeTransferId = null;
        try {
            Transfer transfer = stripeConnectService.payTeacher(teacher, transferAmount, yearMonth);
            stripeTransferId = transfer.getId();
            log.info("Stripe transfer {} created for teacher {} - {} cents", stripeTransferId, teacherId, transferAmount);
        } catch (Exception e) {
            log.error("Failed to create Stripe transfer for teacher {}", teacherId, e);
            throw new RuntimeException("Erreur lors du transfert Stripe: " + e.getMessage());
        }

        // Update teacher balance - subtract transfer amount from available
        balance.setTotalWithdrawnCents(balance.getTotalWithdrawnCents() + transferAmount);
        balance.setAvailableBalanceCents(availableBalance - transferAmount);
        teacherBalanceRepository.save(balance);

        // Update payout record (add to existing if already has amount for this month)
        int existingAmount = payout.getAmountCents() != null ? payout.getAmountCents() : 0;
        payout.setAmountCents(existingAmount + transferAmount);
        payout.setLessonsCount(balance.getLessonsCompleted());
        payout.setIsPaid(balance.getAvailableBalanceCents() == 0); // Mark as paid only if fully paid
        payout.setPaidAt(LocalDateTime.now());
        payout.setPaymentReference(paymentReference);
        payout.setNotes(notes);
        payout.setStripeTransferId(stripeTransferId);

        teacherPayoutRepository.save(payout);
        log.info("Transferred {} cents to teacher {}, remaining balance: {} cents", transferAmount, teacherId, balance.getAvailableBalanceCents());

        // Create payout invoice with PDF
        invoiceService.generatePayoutInvoice(teacher, transferAmount, yearMonth, stripeTransferId);

        return new TeacherPayoutResult(transferAmount, stripeTransferId, balance.getLessonsCompleted());
    }

    /**
     * Result record for teacher payout
     */
    public record TeacherPayoutResult(int amountCents, String stripeTransferId, int lessonsCount) {}

    /**
     * Get all subscriptions
     */
    @Transactional(readOnly = true)
    public Page<Subscription> getSubscriptions(Pageable pageable) {
        return subscriptionRepository.findAll(pageable);
    }

    /**
     * Cancel a subscription
     */
    @Transactional
    public void cancelSubscription(Long subscriptionId, String reason) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found"));

        subscription.setIsActive(false);
        subscription.setCancelledAt(LocalDateTime.now());
        subscriptionRepository.save(subscription);
        log.info("Subscription {} cancelled. Reason: {}", subscriptionId, reason);
    }

    /**
     * Get all payments
     */
    @Transactional(readOnly = true)
    public Page<Payment> getPayments(Pageable pageable) {
        return paymentRepository.findAll(pageable);
    }

    /**
     * Get admin dashboard stats
     * Optimized: Uses aggregate queries instead of loading all lessons into memory
     * Note: totalUsers excludes ADMIN users to match the user list view
     */
    @Transactional(readOnly = true)
    public AdminStatsResponse getStats() {
        long totalStudents = userRepository.countByRole(UserRole.STUDENT);
        long totalTeachers = userRepository.countByRole(UserRole.TEACHER);
        // Exclude admins from total count to match user list view
        long totalUsers = totalStudents + totalTeachers;
        long activeSubscriptions = subscriptionRepository.countByIsActiveTrue();
        long totalLessons = lessonRepository.count();

        // Lessons this month
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime monthStart = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = currentMonth.atEndOfMonth().atTime(23, 59, 59);
        long lessonsThisMonth = lessonRepository.countByScheduledAtBetween(monthStart, monthEnd);

        // Revenue calculations - use aggregate queries
        long totalRevenue = lessonRepository.sumPriceCentsCompleted();
        long revenueThisMonth = lessonRepository.sumPriceCentsCompletedBetween(monthStart, monthEnd);

        return new AdminStatsResponse(
                totalUsers,
                totalStudents,
                totalTeachers,
                activeSubscriptions,
                totalLessons,
                lessonsThisMonth,
                totalRevenue,
                revenueThisMonth
        );
    }

    /**
     * Mask BIC code for security (show only first 4 and last 2 characters).
     * Example: BNPAFRPP -> BNPA**PP
     */
    private String maskBic(String bic) {
        if (bic == null || bic.length() < 6) return bic;
        return bic.substring(0, 4) + "**" + bic.substring(bic.length() - 2);
    }

    /**
     * Mask SIRET number for security (show only last 5 digits).
     * Example: 12345678901234 -> *********01234
     */
    private String maskSiret(String siret) {
        if (siret == null || siret.length() < 5) return siret;
        return "*".repeat(siret.length() - 5) + siret.substring(siret.length() - 5);
    }
}
