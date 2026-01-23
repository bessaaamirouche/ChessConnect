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
import com.chessconnect.model.enums.UserRole;
import com.chessconnect.model.TeacherPayout;
import com.chessconnect.repository.*;
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
            InvoiceRepository invoiceRepository
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
    }

    /**
     * Get all users with pagination and optional role filter
     * Note: ADMIN users are excluded from the list
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

        return users.map(user -> {
            Long lessonsCount = lessonRepository.countByStudentIdOrTeacherId(user.getId());
            Double avgRating = null;
            Long reviewCount = null;
            if (user.getRole() == UserRole.TEACHER) {
                avgRating = ratingRepository.getAverageRatingForTeacher(user.getId());
                Integer count = ratingRepository.getReviewCountForTeacher(user.getId());
                reviewCount = count != null ? count.longValue() : 0L;
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
     * Checks for pending/confirmed lessons before deletion.
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

        // Check for active lessons (PENDING or CONFIRMED) - optimized query
        Long activeLessonsCount = lessonRepository.countActiveLessonsByUserId(userId);

        if (activeLessonsCount > 0) {
            throw new IllegalArgumentException(
                    "Impossible de supprimer cet utilisateur: " + activeLessonsCount + " cours en attente ou confirmes");
        }

        log.info("Deleting user {} ({} {}) and all related data...", userId, user.getFirstName(), user.getLastName());

        // Delete all related data in correct order (FK dependencies)
        // 1. Password reset tokens
        passwordResetTokenRepository.deleteByUserId(userId);
        log.debug("Deleted password reset tokens for user {}", userId);

        // 2. Ratings (as student who gave ratings, or as teacher who received ratings)
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

        // 8. Invoices (as customer or issuer)
        invoiceRepository.deleteByCustomerId(userId);
        invoiceRepository.deleteByIssuerId(userId);
        log.debug("Deleted invoices for user {}", userId);

        // 9. Lessons (as student or teacher) - must be after ratings/payments that reference lessons
        lessonRepository.deleteByStudentId(userId);
        lessonRepository.deleteByTeacherId(userId);
        log.debug("Deleted lessons for user {}", userId);

        // 9. Subscriptions
        subscriptionRepository.deleteByStudentId(userId);
        log.debug("Deleted subscriptions for user {}", userId);

        // 10. Progress
        progressRepository.deleteByStudentId(userId);
        log.debug("Deleted progress for user {}", userId);

        // 11. Finally delete the user
        userRepository.delete(user);
        log.info("User {} deleted successfully", userId);
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
                    // Banking info
                    teacher.getIban(),
                    teacher.getBic(),
                    teacher.getAccountHolderName(),
                    teacher.getSiret(),
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

        // Create payout invoice
        createPayoutInvoice(teacher, transferAmount, yearMonth, stripeTransferId);

        return new TeacherPayoutResult(transferAmount, stripeTransferId, balance.getLessonsCompleted());
    }

    /**
     * Create a payout invoice for a teacher transfer.
     */
    private Invoice createPayoutInvoice(User teacher, int amountCents, String yearMonth, String stripeTransferId) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(generatePayoutInvoiceNumber());
        invoice.setInvoiceType(InvoiceType.PAYOUT_INVOICE);
        invoice.setCustomer(teacher); // Teacher receives this invoice
        invoice.setIssuer(null); // Platform is the issuer
        invoice.setStripePaymentIntentId(stripeTransferId);
        invoice.setSubtotalCents(amountCents);
        invoice.setVatCents(0);
        invoice.setTotalCents(amountCents);
        invoice.setVatRate(0);
        invoice.setDescription("Virement coach - " + yearMonth);
        invoice.setStatus("PAID");
        invoice.setIssuedAt(LocalDateTime.now());

        Invoice saved = invoiceRepository.save(invoice);
        log.info("Created payout invoice {} for teacher {} - {} cents", saved.getInvoiceNumber(), teacher.getId(), amountCents);
        return saved;
    }

    /**
     * Generate a unique sequential invoice number for payouts.
     */
    private String generatePayoutInvoiceNumber() {
        Long maxId = invoiceRepository.findMaxId();
        long nextId = (maxId != null ? maxId : 0) + 1;
        int year = LocalDateTime.now().getYear();
        return String.format("VIR-%d-%06d", year, nextId);
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
     */
    @Transactional(readOnly = true)
    public AdminStatsResponse getStats() {
        long totalUsers = userRepository.count();
        long totalStudents = userRepository.countByRole(UserRole.STUDENT);
        long totalTeachers = userRepository.countByRole(UserRole.TEACHER);
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
}
