package com.chessconnect.service;

import com.chessconnect.config.StripeConfig;
import com.chessconnect.model.User;
import com.chessconnect.model.enums.SubscriptionPlan;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class StripeService {

    private static final Logger log = LoggerFactory.getLogger(StripeService.class);

    private final StripeConfig stripeConfig;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    // Stripe Price IDs (configured in Stripe Dashboard)
    private static final Map<SubscriptionPlan, String> PRICE_IDS = new HashMap<>();

    static {
        // These should be configured via environment variables in production
        PRICE_IDS.put(SubscriptionPlan.BASIC, "price_basic_monthly");
        PRICE_IDS.put(SubscriptionPlan.STANDARD, "price_standard_monthly");
        PRICE_IDS.put(SubscriptionPlan.PREMIUM, "price_premium_monthly");
    }

    public StripeService(StripeConfig stripeConfig) {
        this.stripeConfig = stripeConfig;
    }

    public String createCustomer(User user) throws StripeException {
        CustomerCreateParams params = CustomerCreateParams.builder()
                .setEmail(user.getEmail())
                .setName(user.getFullName())
                .putMetadata("user_id", user.getId().toString())
                .build();

        Customer customer = Customer.create(params);
        log.info("Created Stripe customer {} for user {}", customer.getId(), user.getId());
        return customer.getId();
    }

    public Session createCheckoutSession(User user, SubscriptionPlan plan, String customerId) throws StripeException {
        // Use dynamic price data instead of pre-created price IDs
        SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setSuccessUrl(frontendUrl + "/subscription/success?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(frontendUrl + "/subscription/cancel")
                .putMetadata("user_id", user.getId().toString())
                .putMetadata("plan", plan.name())
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("eur")
                                                .setUnitAmount((long) plan.getPriceCents())
                                                .setRecurring(
                                                        SessionCreateParams.LineItem.PriceData.Recurring.builder()
                                                                .setInterval(SessionCreateParams.LineItem.PriceData.Recurring.Interval.MONTH)
                                                                .build()
                                                )
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName("Abonnement " + plan.getDisplayName())
                                                                .setDescription("ChessConnect - " + plan.getMonthlyQuota() + " cours par mois")
                                                                .build()
                                                )
                                                .build()
                                )
                                .setQuantity(1L)
                                .build()
                );

        if (customerId != null) {
            paramsBuilder.setCustomer(customerId);
        } else {
            paramsBuilder.setCustomerEmail(user.getEmail());
        }

        Session session = Session.create(paramsBuilder.build());
        log.info("Created checkout session {} for user {} with plan {}", session.getId(), user.getId(), plan);
        return session;
    }

    public Session createOneTimePaymentSession(User user, int amountCents, String description, Long lessonId) throws StripeException {
        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(frontendUrl + "/lessons/book/success?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(frontendUrl + "/lessons/book/cancel")
                .setCustomerEmail(user.getEmail())
                .putMetadata("user_id", user.getId().toString())
                .putMetadata("lesson_id", lessonId != null ? lessonId.toString() : "")
                .putMetadata("type", "ONE_TIME_LESSON")
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("eur")
                                                .setUnitAmount((long) amountCents)
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName(description)
                                                                .setDescription("Cours d'échecs - ChessConnect")
                                                                .build()
                                                )
                                                .build()
                                )
                                .setQuantity(1L)
                                .build()
                )
                .build();

        Session session = Session.create(params);
        log.info("Created one-time payment session {} for user {}", session.getId(), user.getId());
        return session;
    }

    public Session createLessonPaymentSession(
            User student,
            Long teacherId,
            int amountCents,
            String description,
            String scheduledAt,
            int durationMinutes,
            String notes
    ) throws StripeException {
        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(frontendUrl + "/lessons/payment/success?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(frontendUrl + "/lessons/payment/cancel")
                .setCustomerEmail(student.getEmail())
                .putMetadata("user_id", student.getId().toString())
                .putMetadata("teacher_id", teacherId.toString())
                .putMetadata("scheduled_at", scheduledAt)
                .putMetadata("duration_minutes", String.valueOf(durationMinutes))
                .putMetadata("notes", notes != null ? notes : "")
                .putMetadata("type", "ONE_TIME_LESSON")
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("eur")
                                                .setUnitAmount((long) amountCents)
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName(description)
                                                                .setDescription("Cours d'échecs - ChessConnect")
                                                                .build()
                                                )
                                                .build()
                                )
                                .setQuantity(1L)
                                .build()
                )
                .build();

        Session session = Session.create(params);
        log.info("Created lesson payment session {} for user {} with teacher {}",
                session.getId(), student.getId(), teacherId);
        return session;
    }

    public Session retrieveSession(String sessionId) throws StripeException {
        return Session.retrieve(sessionId);
    }

    public Subscription retrieveSubscription(String subscriptionId) throws StripeException {
        return Subscription.retrieve(subscriptionId);
    }

    public Subscription cancelSubscription(String subscriptionId) throws StripeException {
        Subscription subscription = Subscription.retrieve(subscriptionId);
        return subscription.cancel();
    }

    public Event constructEvent(String payload, String sigHeader) throws SignatureVerificationException {
        return Webhook.constructEvent(payload, sigHeader, stripeConfig.getWebhookSecret());
    }

    public String getPublishableKey() {
        return stripeConfig.getPublishableKey();
    }

    private String getPriceIdForPlan(SubscriptionPlan plan) {
        String priceId = PRICE_IDS.get(plan);
        if (priceId == null) {
            throw new IllegalArgumentException("Unknown subscription plan: " + plan);
        }
        return priceId;
    }

    public void setPriceId(SubscriptionPlan plan, String priceId) {
        PRICE_IDS.put(plan, priceId);
    }

    /**
     * Create a refund for a payment.
     * @param paymentIntentId The Stripe PaymentIntent ID
     * @param amountCents Amount to refund in cents (null for full refund)
     * @param reason Reason for the refund
     * @return The Stripe Refund object
     */
    public Refund createRefund(String paymentIntentId, Long amountCents, String reason) throws StripeException {
        RefundCreateParams.Builder paramsBuilder = RefundCreateParams.builder()
                .setPaymentIntent(paymentIntentId);

        if (amountCents != null) {
            paramsBuilder.setAmount(amountCents);
        }

        if (reason != null) {
            paramsBuilder.putMetadata("reason", reason);
        }

        Refund refund = Refund.create(paramsBuilder.build());
        log.info("Created refund {} for PaymentIntent {} - Amount: {} cents",
                refund.getId(), paymentIntentId, amountCents != null ? amountCents : "full");
        return refund;
    }

    /**
     * Create a partial refund based on percentage.
     * @param paymentIntentId The Stripe PaymentIntent ID
     * @param originalAmountCents The original payment amount
     * @param refundPercentage Percentage to refund (0-100)
     * @param reason Reason for the refund
     * @return The Stripe Refund object
     */
    public Refund createPartialRefund(String paymentIntentId, int originalAmountCents, int refundPercentage, String reason) throws StripeException {
        if (refundPercentage <= 0) {
            log.info("No refund needed (0%) for PaymentIntent {}", paymentIntentId);
            return null;
        }

        long refundAmount = (originalAmountCents * refundPercentage) / 100;
        return createRefund(paymentIntentId, refundAmount, reason);
    }
}
