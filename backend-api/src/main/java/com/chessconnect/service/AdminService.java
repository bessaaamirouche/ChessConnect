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

    public AdminService(
            UserRepository userRepository,
            LessonRepository lessonRepository,
            SubscriptionRepository subscriptionRepository,
            PaymentRepository paymentRepository,
            TeacherBalanceRepository teacherBalanceRepository,
            RatingRepository ratingRepository,
            TeacherPayoutRepository teacherPayoutRepository
    ) {
        this.userRepository = userRepository;
        this.lessonRepository = lessonRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.paymentRepository = paymentRepository;
        this.teacherBalanceRepository = teacherBalanceRepository;
        this.ratingRepository = ratingRepository;
        this.teacherPayoutRepository = teacherPayoutRepository;
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

        log.info("Deleting user {} ({} {})", userId, user.getFirstName(), user.getLastName());
        userRepository.delete(user);
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
                    currentMonthLessons.size()
            );
        }).toList();
    }

    /**
     * Mark a teacher as paid for a specific month
     */
    @Transactional
    public void markTeacherPaid(Long teacherId, String yearMonth, String paymentReference, String notes) {
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found"));

        // Get or create payout record
        TeacherPayout payout = teacherPayoutRepository
                .findByTeacherIdAndYearMonth(teacherId, yearMonth)
                .orElseGet(() -> {
                    TeacherPayout newPayout = new TeacherPayout();
                    newPayout.setTeacher(teacher);
                    newPayout.setYearMonth(yearMonth);
                    return newPayout;
                });

        // Calculate earnings for that month
        YearMonth ym = YearMonth.parse(yearMonth);
        LocalDateTime monthStart = ym.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = ym.atEndOfMonth().atTime(23, 59, 59);

        List<Lesson> monthLessons = lessonRepository.findAll().stream()
                .filter(l -> l.getTeacher().getId().equals(teacherId))
                .filter(l -> l.getStatus() == LessonStatus.COMPLETED)
                .filter(l -> l.getScheduledAt().isAfter(monthStart) && l.getScheduledAt().isBefore(monthEnd))
                .toList();

        int earnings = monthLessons.stream()
                .mapToInt(l -> l.getTeacherEarningsCents() != null ? l.getTeacherEarningsCents() : 0)
                .sum();

        payout.setAmountCents(earnings);
        payout.setLessonsCount(monthLessons.size());
        payout.setIsPaid(true);
        payout.setPaidAt(LocalDateTime.now());
        payout.setPaymentReference(paymentReference);
        payout.setNotes(notes);

        teacherPayoutRepository.save(payout);
        log.info("Marked teacher {} as paid for {}: {} cents", teacherId, yearMonth, earnings);
    }

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
