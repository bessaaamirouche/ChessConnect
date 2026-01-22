package com.chessconnect.service;

import com.chessconnect.dto.admin.*;
import com.chessconnect.dto.lesson.LessonResponse;
import com.chessconnect.model.Lesson;
import com.chessconnect.model.Payment;
import com.chessconnect.model.Subscription;
import com.chessconnect.model.TeacherBalance;
import com.chessconnect.model.User;
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
            ProgressRepository progressRepository
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
    }

    /**
     * Get all users with pagination and optional role filter
     * Note: ADMIN users are excluded from the list
     */
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

        // Check for active lessons (PENDING or CONFIRMED)
        List<Lesson> activeLessons = lessonRepository.findAll().stream()
                .filter(l -> (l.getStudent().getId().equals(userId) || l.getTeacher().getId().equals(userId))
                        && (l.getStatus() == LessonStatus.PENDING || l.getStatus() == LessonStatus.CONFIRMED))
                .toList();

        if (!activeLessons.isEmpty()) {
            throw new IllegalArgumentException(
                    "Impossible de supprimer cet utilisateur: " + activeLessons.size() + " cours en attente ou confirmes");
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

        // 8. Lessons (as student or teacher) - must be after ratings/payments that reference lessons
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
     */
    public List<LessonResponse> getUpcomingLessons() {
        return lessonRepository.findAll().stream()
                .filter(l -> l.getStatus() == LessonStatus.PENDING || l.getStatus() == LessonStatus.CONFIRMED)
                .filter(l -> l.getScheduledAt().isAfter(LocalDateTime.now().minusHours(1)))
                .sorted(Comparator.comparing(Lesson::getScheduledAt))
                .map(LessonResponse::from)
                .toList();
    }

    /**
     * Get completed lessons - ordered by scheduled date descending
     */
    public List<LessonResponse> getCompletedLessons() {
        return lessonRepository.findAll().stream()
                .filter(l -> l.getStatus() == LessonStatus.COMPLETED)
                .sorted(Comparator.comparing(Lesson::getScheduledAt).reversed())
                .map(LessonResponse::from)
                .toList();
    }

    /**
     * Get accounting/revenue overview
     */
    public AccountingResponse getAccountingOverview() {
        List<Lesson> allLessons = lessonRepository.findAll();

        long totalRevenue = 0;
        long totalCommissions = 0;
        long totalTeacherEarnings = 0;
        long totalRefunded = 0;
        long completedLessons = 0;
        long cancelledLessons = 0;

        for (Lesson lesson : allLessons) {
            if (lesson.getStatus() == LessonStatus.COMPLETED) {
                completedLessons++;
                totalRevenue += lesson.getPriceCents() != null ? lesson.getPriceCents() : 0;
                totalCommissions += lesson.getCommissionCents() != null ? lesson.getCommissionCents() : 0;
                totalTeacherEarnings += lesson.getTeacherEarningsCents() != null ? lesson.getTeacherEarningsCents() : 0;
            } else if (lesson.getStatus() == LessonStatus.CANCELLED) {
                cancelledLessons++;
                totalRefunded += lesson.getRefundedAmountCents() != null ? lesson.getRefundedAmountCents() : 0;
            }
        }

        return new AccountingResponse(
                totalRevenue,
                totalCommissions,
                totalTeacherEarnings,
                totalRefunded,
                (long) allLessons.size(),
                completedLessons,
                cancelledLessons
        );
    }

    /**
     * Get all teacher balances with banking info and current month payout status
     */
    public List<TeacherBalanceListResponse> getTeacherBalances() {
        List<TeacherBalance> balances = teacherBalanceRepository.findAll();
        String currentYearMonth = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

        // Get current month lessons for all teachers
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime monthStart = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = currentMonth.atEndOfMonth().atTime(23, 59, 59);

        return balances.stream().map(balance -> {
            User teacher = balance.getTeacher();

            // Calculate current month earnings
            List<Lesson> currentMonthLessons = lessonRepository.findAll().stream()
                    .filter(l -> l.getTeacher().getId().equals(teacher.getId()))
                    .filter(l -> l.getStatus() == LessonStatus.COMPLETED)
                    .filter(l -> l.getScheduledAt().isAfter(monthStart) && l.getScheduledAt().isBefore(monthEnd))
                    .toList();

            int currentMonthEarnings = currentMonthLessons.stream()
                    .mapToInt(l -> l.getTeacherEarningsCents() != null ? l.getTeacherEarningsCents() : 0)
                    .sum();

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
                    currentMonthEarnings,
                    currentMonthLessons.size(),
                    // Stripe Connect status
                    stripeConnectEnabled,
                    stripeConnectReady
            );
        }).toList();
    }

    /**
     * Mark a teacher as paid - transfers the FULL available balance.
     * This performs a real Stripe Connect transfer to the teacher's connected account.
     */
    @Transactional
    public TeacherPayoutResult markTeacherPaid(Long teacherId, String yearMonth, String paymentReference, String notes) {
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new IllegalArgumentException("Coach non trouve"));

        // Verify teacher has Stripe Connect set up
        if (teacher.getStripeConnectAccountId() == null || teacher.getStripeConnectAccountId().isBlank()) {
            throw new IllegalArgumentException("Le coach n'a pas configure son compte Stripe Connect");
        }

        // Get teacher's balance
        TeacherBalance balance = teacherBalanceRepository.findByTeacherId(teacherId)
                .orElseThrow(() -> new IllegalArgumentException("Aucun solde trouve pour ce coach"));

        // Use the FULL available balance (not just current month)
        int availableBalance = balance.getAvailableBalanceCents() != null ? balance.getAvailableBalanceCents() : 0;

        if (availableBalance <= 0) {
            throw new IllegalArgumentException("Aucun solde disponible a transferer");
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

        // Check if already paid this month
        if (payout.getIsPaid() != null && payout.getIsPaid()) {
            throw new IllegalArgumentException("Le coach a deja ete paye pour ce mois");
        }

        // Perform the Stripe transfer with FULL available balance
        String stripeTransferId = null;
        try {
            Transfer transfer = stripeConnectService.payTeacher(teacher, availableBalance, yearMonth);
            stripeTransferId = transfer.getId();
            log.info("Stripe transfer {} created for teacher {} - {} cents (full balance)", stripeTransferId, teacherId, availableBalance);
        } catch (Exception e) {
            log.error("Failed to create Stripe transfer for teacher {}", teacherId, e);
            throw new RuntimeException("Erreur lors du transfert Stripe: " + e.getMessage());
        }

        // Update teacher balance - reset available to 0, add to withdrawn
        balance.setTotalWithdrawnCents(balance.getTotalWithdrawnCents() + availableBalance);
        balance.setAvailableBalanceCents(0);
        teacherBalanceRepository.save(balance);

        // Update payout record
        payout.setAmountCents(availableBalance);
        payout.setLessonsCount(balance.getLessonsCompleted());
        payout.setIsPaid(true);
        payout.setPaidAt(LocalDateTime.now());
        payout.setPaymentReference(paymentReference);
        payout.setNotes(notes);
        payout.setStripeTransferId(stripeTransferId);

        teacherPayoutRepository.save(payout);
        log.info("Marked teacher {} as paid: {} cents transferred, balance reset to 0", teacherId, availableBalance);

        return new TeacherPayoutResult(availableBalance, stripeTransferId, balance.getLessonsCompleted());
    }

    /**
     * Result record for teacher payout
     */
    public record TeacherPayoutResult(int amountCents, String stripeTransferId, int lessonsCount) {}

    /**
     * Get all subscriptions
     */
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
    public Page<Payment> getPayments(Pageable pageable) {
        return paymentRepository.findAll(pageable);
    }

    /**
     * Get admin dashboard stats
     */
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

        // Revenue calculations
        List<Lesson> completedLessons = lessonRepository.findAll().stream()
                .filter(l -> l.getStatus() == LessonStatus.COMPLETED)
                .toList();

        long totalRevenue = completedLessons.stream()
                .mapToLong(l -> l.getPriceCents() != null ? l.getPriceCents() : 0)
                .sum();

        long revenueThisMonth = completedLessons.stream()
                .filter(l -> l.getScheduledAt().isAfter(monthStart) && l.getScheduledAt().isBefore(monthEnd))
                .mapToLong(l -> l.getPriceCents() != null ? l.getPriceCents() : 0)
                .sum();

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
