package com.chessconnect.service;

import com.chessconnect.dto.admin.*;
import com.chessconnect.model.Lesson;
import com.chessconnect.model.Payment;
import com.chessconnect.model.Subscription;
import com.chessconnect.model.TeacherBalance;
import com.chessconnect.model.User;
import com.chessconnect.model.enums.LessonStatus;
import com.chessconnect.model.enums.UserRole;
import com.chessconnect.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
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

    public AdminService(
            UserRepository userRepository,
            LessonRepository lessonRepository,
            SubscriptionRepository subscriptionRepository,
            PaymentRepository paymentRepository,
            TeacherBalanceRepository teacherBalanceRepository,
            RatingRepository ratingRepository
    ) {
        this.userRepository = userRepository;
        this.lessonRepository = lessonRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.paymentRepository = paymentRepository;
        this.teacherBalanceRepository = teacherBalanceRepository;
        this.ratingRepository = ratingRepository;
    }

    /**
     * Get all users with pagination and optional role filter
     */
    public Page<UserListResponse> getUsers(Pageable pageable, String roleFilter) {
        Page<User> users;
        if (roleFilter != null && !roleFilter.isBlank()) {
            UserRole role = UserRole.valueOf(roleFilter.toUpperCase());
            users = userRepository.findByRole(role, pageable);
        } else {
            users = userRepository.findAll(pageable);
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
     * Get all teacher balances
     */
    public List<TeacherBalanceListResponse> getTeacherBalances() {
        List<TeacherBalance> balances = teacherBalanceRepository.findAll();
        return balances.stream().map(balance -> {
            User teacher = balance.getTeacher();
            return new TeacherBalanceListResponse(
                    teacher.getId(),
                    teacher.getFirstName(),
                    teacher.getLastName(),
                    teacher.getEmail(),
                    balance.getAvailableBalanceCents(),
                    balance.getPendingBalanceCents(),
                    balance.getTotalEarnedCents(),
                    balance.getTotalWithdrawnCents(),
                    balance.getLessonsCompleted()
            );
        }).toList();
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
