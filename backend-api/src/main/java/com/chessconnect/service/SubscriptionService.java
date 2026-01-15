package com.chessconnect.service;

import com.chessconnect.dto.payment.CheckoutSessionResponse;
import com.chessconnect.dto.subscription.SubscriptionResponse;
import com.chessconnect.model.Payment;
import com.chessconnect.model.Subscription;
import com.chessconnect.model.User;
import com.chessconnect.model.enums.PaymentStatus;
import com.chessconnect.model.enums.PaymentType;
import com.chessconnect.model.enums.SubscriptionPlan;
import com.chessconnect.repository.PaymentRepository;
import com.chessconnect.repository.SubscriptionRepository;
import com.chessconnect.repository.UserRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class SubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final StripeService stripeService;

    public SubscriptionService(
            SubscriptionRepository subscriptionRepository,
            UserRepository userRepository,
            PaymentRepository paymentRepository,
            StripeService stripeService
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.userRepository = userRepository;
        this.paymentRepository = paymentRepository;
        this.stripeService = stripeService;
    }

    public CheckoutSessionResponse createSubscriptionCheckout(Long userId, SubscriptionPlan plan) throws StripeException {
        return createSubscriptionCheckout(userId, plan, false);
    }

    public CheckoutSessionResponse createSubscriptionCheckout(Long userId, SubscriptionPlan plan, boolean embedded) throws StripeException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if user already has an active subscription
        List<Subscription> existingSubs = subscriptionRepository.findActiveSubscriptionsByStudentId(userId);
        if (!existingSubs.isEmpty()) {
            throw new RuntimeException("User already has an active subscription");
        }

        Session session = stripeService.createCheckoutSession(user, plan, null, embedded);

        CheckoutSessionResponse.CheckoutSessionResponseBuilder responseBuilder = CheckoutSessionResponse.builder()
                .sessionId(session.getId())
                .publishableKey(stripeService.getPublishableKey());

        if (embedded) {
            responseBuilder.clientSecret(session.getClientSecret());
        } else {
            responseBuilder.url(session.getUrl());
        }

        return responseBuilder.build();
    }

    @Transactional
    public SubscriptionResponse activateSubscription(String stripeSubscriptionId, Long userId, SubscriptionPlan plan) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Subscription subscription = new Subscription();
        subscription.setStudent(user);
        subscription.setPlanType(plan);
        subscription.setPriceCents(plan.getPriceCents());
        subscription.setMonthlyQuota(plan.getMonthlyQuota());
        subscription.setLessonsUsedThisMonth(0);
        subscription.setStartDate(LocalDate.now());
        subscription.setIsActive(true);
        subscription.setStripeSubscriptionId(stripeSubscriptionId);

        subscription = subscriptionRepository.save(subscription);

        // Create payment record
        Payment payment = new Payment();
        payment.setPayer(user);
        payment.setSubscription(subscription);
        payment.setPaymentType(PaymentType.SUBSCRIPTION);
        payment.setAmountCents(plan.getPriceCents());
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setProcessedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        log.info("Activated subscription {} for user {} with plan {}", subscription.getId(), userId, plan);

        return SubscriptionResponse.fromEntity(subscription);
    }

    public Optional<SubscriptionResponse> getActiveSubscription(Long userId) {
        return subscriptionRepository.findActiveSubscriptionsByStudentId(userId)
                .stream()
                .findFirst()
                .map(SubscriptionResponse::fromEntity);
    }

    public List<SubscriptionResponse> getSubscriptionHistory(Long userId) {
        return subscriptionRepository.findByStudentIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(SubscriptionResponse::fromEntity)
                .toList();
    }

    @Transactional
    public SubscriptionResponse cancelSubscription(Long userId) throws StripeException {
        Subscription subscription = subscriptionRepository.findActiveSubscriptionsByStudentId(userId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No active subscription found"));

        // Cancel in Stripe (at end of period)
        if (subscription.getStripeSubscriptionId() != null) {
            stripeService.cancelSubscription(subscription.getStripeSubscriptionId());
        }

        // Set cancellation date
        subscription.setCancelledAt(LocalDateTime.now());

        // Set end date to end of current billing period
        // The subscription remains active until this date
        LocalDate endDate = subscription.getStartDate()
                .plusMonths(1)
                .with(TemporalAdjusters.firstDayOfMonth());
        subscription.setEndDate(endDate);

        // Keep isActive = true so user can still use remaining lessons until endDate

        subscription = subscriptionRepository.save(subscription);

        log.info("Cancelled subscription {} for user {}. Active until {}",
                subscription.getId(), userId, endDate);

        return SubscriptionResponse.fromEntity(subscription);
    }

    @Transactional
    public void useLesson(Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));

        if (!subscription.hasRemainingLessons()) {
            throw new RuntimeException("No remaining lessons in subscription");
        }

        subscription.setLessonsUsedThisMonth(subscription.getLessonsUsedThisMonth() + 1);
        subscriptionRepository.save(subscription);

        log.debug("Used lesson from subscription {}. Remaining: {}",
                subscriptionId, subscription.getRemainingLessons());
    }

    @Transactional
    public void restoreLesson(Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));

        if (subscription.getLessonsUsedThisMonth() > 0) {
            subscription.setLessonsUsedThisMonth(subscription.getLessonsUsedThisMonth() - 1);
            subscriptionRepository.save(subscription);
            log.debug("Restored lesson to subscription {}. Remaining: {}",
                    subscriptionId, subscription.getRemainingLessons());
        }
    }

    // Reset monthly quotas on the 1st of each month at midnight
    @Scheduled(cron = "0 0 0 1 * *")
    @Transactional
    public void resetMonthlyQuotas() {
        List<Subscription> activeSubscriptions = subscriptionRepository.findAllByIsActiveTrue();

        for (Subscription sub : activeSubscriptions) {
            sub.setLessonsUsedThisMonth(0);
        }

        subscriptionRepository.saveAll(activeSubscriptions);
        log.info("Reset monthly quotas for {} active subscriptions", activeSubscriptions.size());
    }

    // Deactivate expired subscriptions daily at midnight
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void deactivateExpiredSubscriptions() {
        List<Subscription> activeSubscriptions = subscriptionRepository.findAllByIsActiveTrue();
        LocalDate today = LocalDate.now();
        int deactivatedCount = 0;

        for (Subscription sub : activeSubscriptions) {
            if (sub.getEndDate() != null && !today.isBefore(sub.getEndDate())) {
                sub.setIsActive(false);
                deactivatedCount++;
            }
        }

        if (deactivatedCount > 0) {
            subscriptionRepository.saveAll(activeSubscriptions);
            log.info("Deactivated {} expired subscriptions", deactivatedCount);
        }
    }

    public boolean hasActiveSubscription(Long userId) {
        return !subscriptionRepository.findActiveSubscriptionsByStudentId(userId).isEmpty();
    }

    public boolean canUseSubscriptionLesson(Long userId) {
        return subscriptionRepository.findActiveSubscriptionsByStudentId(userId)
                .stream()
                .findFirst()
                .map(Subscription::hasRemainingLessons)
                .orElse(false);
    }

    /**
     * Admin method to clean up duplicate active subscriptions.
     * Keeps only the most recent active subscription per student and deactivates the rest.
     */
    @Transactional
    public int cleanupDuplicateSubscriptions() {
        List<Subscription> allActiveSubscriptions = subscriptionRepository.findAllByIsActiveTrue();

        // Group by student ID
        Map<Long, List<Subscription>> subscriptionsByStudent = allActiveSubscriptions.stream()
                .collect(java.util.stream.Collectors.groupingBy(s -> s.getStudent().getId()));

        int deactivatedCount = 0;

        for (Map.Entry<Long, List<Subscription>> entry : subscriptionsByStudent.entrySet()) {
            List<Subscription> studentSubs = entry.getValue();

            if (studentSubs.size() > 1) {
                // Sort by createdAt descending (most recent first)
                studentSubs.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));

                // Keep the first one (most recent), deactivate the rest
                for (int i = 1; i < studentSubs.size(); i++) {
                    Subscription duplicate = studentSubs.get(i);
                    duplicate.setIsActive(false);
                    subscriptionRepository.save(duplicate);
                    deactivatedCount++;
                    log.info("Deactivated duplicate subscription {} for student {}",
                            duplicate.getId(), entry.getKey());
                }
            }
        }

        log.info("Cleaned up {} duplicate subscriptions", deactivatedCount);
        return deactivatedCount;
    }
}
