package com.chessconnect.controller;

import com.chessconnect.dto.lesson.BookLessonRequest;
import com.chessconnect.dto.lesson.LessonResponse;
import com.chessconnect.dto.payment.CheckoutSessionResponse;
import com.chessconnect.dto.payment.CreateCheckoutSessionRequest;
import com.chessconnect.dto.payment.CreateLessonCheckoutRequest;
import com.chessconnect.dto.payment.PaymentResponse;
import com.chessconnect.dto.subscription.SubscriptionPlanResponse;
import com.chessconnect.dto.subscription.SubscriptionResponse;
import com.chessconnect.model.Lesson;
import com.chessconnect.model.Payment;
import com.chessconnect.model.User;
import com.chessconnect.model.enums.PaymentStatus;
import com.chessconnect.model.enums.PaymentType;
import com.chessconnect.model.enums.SubscriptionPlan;
import com.chessconnect.repository.LessonRepository;
import com.chessconnect.repository.PaymentRepository;
import com.chessconnect.repository.UserRepository;
import com.chessconnect.security.UserDetailsImpl;
import com.chessconnect.service.InvoiceService;
import com.chessconnect.service.LessonService;
import com.chessconnect.service.StripeService;
import com.chessconnect.service.SubscriptionService;

import java.time.LocalDateTime;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final StripeService stripeService;
    private final SubscriptionService subscriptionService;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final LessonRepository lessonRepository;
    private final LessonService lessonService;
    private final InvoiceService invoiceService;

    public PaymentController(
            StripeService stripeService,
            SubscriptionService subscriptionService,
            PaymentRepository paymentRepository,
            UserRepository userRepository,
            LessonRepository lessonRepository,
            LessonService lessonService,
            InvoiceService invoiceService
    ) {
        this.stripeService = stripeService;
        this.subscriptionService = subscriptionService;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.lessonRepository = lessonRepository;
        this.lessonService = lessonService;
        this.invoiceService = invoiceService;
    }

    // Get Stripe publishable key
    @GetMapping("/config")
    public ResponseEntity<Map<String, String>> getStripeConfig() {
        return ResponseEntity.ok(Map.of("publishableKey", stripeService.getPublishableKey()));
    }

    // Create checkout session for one-time lesson payment
    @PostMapping("/checkout/lesson")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<CheckoutSessionResponse> createLessonCheckout(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody CreateLessonCheckoutRequest request
    ) {
        try {
            User student = userRepository.findById(userDetails.getId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            User teacher = userRepository.findById(request.getTeacherId())
                    .orElseThrow(() -> new RuntimeException("Teacher not found"));

            int amountCents = teacher.getHourlyRateCents();
            String description = String.format("Cours d'échecs avec %s", teacher.getFullName());

            Session session = stripeService.createLessonPaymentSession(
                    student,
                    teacher.getId(),
                    amountCents,
                    description,
                    request.getScheduledAt().toString(),
                    request.getDurationMinutes(),
                    request.getNotes(),
                    request.isEmbedded()
            );

            CheckoutSessionResponse.CheckoutSessionResponseBuilder responseBuilder = CheckoutSessionResponse.builder()
                    .sessionId(session.getId())
                    .publishableKey(stripeService.getPublishableKey());

            if (request.isEmbedded()) {
                responseBuilder.clientSecret(session.getClientSecret());
            } else {
                responseBuilder.url(session.getUrl());
            }

            return ResponseEntity.ok(responseBuilder.build());
        } catch (StripeException e) {
            log.error("Stripe error creating lesson checkout session", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        } catch (RuntimeException e) {
            log.error("Error creating lesson checkout session", e);
            return ResponseEntity.badRequest().build();
        }
    }

    // Get available subscription plans
    @GetMapping("/plans")
    public ResponseEntity<List<SubscriptionPlanResponse>> getSubscriptionPlans() {
        List<SubscriptionPlanResponse> plans = Arrays.stream(SubscriptionPlan.values())
                .map(SubscriptionPlanResponse::fromEnum)
                .toList();
        return ResponseEntity.ok(plans);
    }

    // Create checkout session for subscription
    @PostMapping("/checkout/subscription")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<CheckoutSessionResponse> createSubscriptionCheckout(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody CreateCheckoutSessionRequest request
    ) {
        try {
            CheckoutSessionResponse response = subscriptionService.createSubscriptionCheckout(
                    userDetails.getId(),
                    request.getPlan(),
                    request.isEmbedded()
            );
            return ResponseEntity.ok(response);
        } catch (StripeException e) {
            log.error("Stripe error creating checkout session", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(null);
        } catch (RuntimeException e) {
            log.error("Error creating checkout session", e);
            return ResponseEntity.badRequest().build();
        }
    }

    // Get current user's active subscription
    @GetMapping("/subscription")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<SubscriptionResponse> getActiveSubscription(
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        return subscriptionService.getActiveSubscription(userDetails.getId())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    // Get subscription history
    @GetMapping("/subscription/history")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<SubscriptionResponse>> getSubscriptionHistory(
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        List<SubscriptionResponse> history = subscriptionService.getSubscriptionHistory(userDetails.getId());
        return ResponseEntity.ok(history);
    }

    // Cancel subscription
    @PostMapping("/subscription/cancel")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<SubscriptionResponse> cancelSubscription(
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        try {
            SubscriptionResponse response = subscriptionService.cancelSubscription(userDetails.getId());
            return ResponseEntity.ok(response);
        } catch (StripeException e) {
            log.error("Stripe error cancelling subscription", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        } catch (RuntimeException e) {
            log.error("Error cancelling subscription", e);
            return ResponseEntity.badRequest().build();
        }
    }

    // Get user's payment history
    @GetMapping("/history")
    public ResponseEntity<List<PaymentResponse>> getPaymentHistory(
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        List<PaymentResponse> payments = paymentRepository.findByPayerIdOrderByCreatedAtDesc(userDetails.getId())
                .stream()
                .map(PaymentResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(payments);
    }

    // Verify checkout session (after redirect)
    @GetMapping("/checkout/verify")
    public ResponseEntity<Map<String, Object>> verifyCheckoutSession(@RequestParam String sessionId) {
        try {
            Session session = stripeService.retrieveSession(sessionId);
            return ResponseEntity.ok(Map.of(
                    "status", session.getStatus(),
                    "paymentStatus", session.getPaymentStatus(),
                    "customerEmail", session.getCustomerEmail() != null ? session.getCustomerEmail() : ""
            ));
        } catch (StripeException e) {
            log.error("Error verifying checkout session", e);
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid session"));
        }
    }

    // Confirm subscription payment and activate subscription (called after Stripe redirect)
    // No auth required - we verify the user via Stripe session metadata
    @PostMapping("/checkout/subscription/confirm")
    public ResponseEntity<Map<String, Object>> confirmSubscriptionPayment(
            @RequestParam String sessionId
    ) {
        try {
            Session session = stripeService.retrieveSession(sessionId);

            // Verify payment is complete
            if (!"paid".equals(session.getPaymentStatus()) && !"complete".equals(session.getStatus())) {
                log.warn("Subscription payment not completed for session {}", sessionId);
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Le paiement n'a pas été effectué"
                ));
            }

            // Get metadata from session
            Map<String, String> metadata = session.getMetadata();
            String planName = metadata.get("plan");
            String userIdStr = metadata.get("user_id");

            if (planName == null || userIdStr == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Session invalide - données manquantes"
                ));
            }

            Long userId = Long.parseLong(userIdStr);

            // Get Stripe subscription ID
            String stripeSubscriptionId = session.getSubscription();

            // Activate subscription
            SubscriptionPlan plan = SubscriptionPlan.valueOf(planName);
            var subscription = subscriptionService.activateSubscription(stripeSubscriptionId, userId, plan);

            log.info("Subscription {} activated for student {} after payment confirmation", subscription.getId(), userId);

            // Generate subscription invoice
            int amountCents = session.getAmountTotal() != null ? session.getAmountTotal().intValue() : plan.getPriceCents();
            String paymentIntentId = session.getPaymentIntent();
            if (paymentIntentId != null) {
                invoiceService.generateSubscriptionInvoice(userId, amountCents, paymentIntentId);
                log.info("Subscription invoice generated for user {}", userId);
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "subscriptionId", subscription.getId(),
                    "planName", plan.getDisplayName(),
                    "message", "Abonnement Premium activé avec succès"
            ));

        } catch (StripeException e) {
            log.error("Stripe error confirming subscription payment", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                    "success", false,
                    "error", "Erreur de vérification du paiement"
            ));
        } catch (Exception e) {
            log.error("Error confirming subscription payment", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    // Confirm lesson payment and book the lesson (called after Stripe redirect)
    // No auth required - we verify the user via Stripe session metadata
    @PostMapping("/checkout/lesson/confirm")
    public ResponseEntity<Map<String, Object>> confirmLessonPayment(
            @RequestParam String sessionId
    ) {
        try {
            Session session = stripeService.retrieveSession(sessionId);

            // Verify payment is complete
            if (!"paid".equals(session.getPaymentStatus())) {
                log.warn("Payment not completed for session {}", sessionId);
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Le paiement n'a pas été effectué"
                ));
            }

            // Get metadata from session
            Map<String, String> metadata = session.getMetadata();
            String type = metadata.get("type");
            String userIdStr = metadata.get("user_id");

            if (!"ONE_TIME_LESSON".equals(type) || userIdStr == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Session invalide"
                ));
            }

            Long userId = Long.parseLong(userIdStr);

            // Extract lesson details from metadata
            Long teacherId = Long.parseLong(metadata.get("teacher_id"));
            LocalDateTime scheduledAt = LocalDateTime.parse(metadata.get("scheduled_at"));
            int durationMinutes = Integer.parseInt(metadata.get("duration_minutes"));
            String notes = metadata.get("notes");

            // Book the lesson
            BookLessonRequest bookRequest = new BookLessonRequest(
                    teacherId,
                    scheduledAt,
                    durationMinutes,
                    notes,
                    false // Don't use subscription - this is a paid lesson
            );

            var lesson = lessonService.bookLesson(userId, bookRequest);
            log.info("Lesson {} booked for student {} after payment confirmation", lesson.id(), userId);

            // Generate invoices
            String paymentIntentId = session.getPaymentIntent();
            int amountCents = session.getAmountTotal() != null ? session.getAmountTotal().intValue() : 0;
            boolean promoApplied = "true".equals(metadata.get("promo_applied"));

            if (paymentIntentId != null && amountCents > 0) {
                invoiceService.generateInvoicesForPayment(
                        paymentIntentId,
                        userId,
                        teacherId,
                        lesson.id(),
                        amountCents,
                        promoApplied
                );
                log.info("Lesson invoices generated for lesson {}", lesson.id());
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "lessonId", lesson.id(),
                    "message", "Cours réservé avec succès"
            ));

        } catch (StripeException e) {
            log.error("Stripe error confirming lesson payment", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                    "success", false,
                    "error", "Erreur de vérification du paiement"
            ));
        } catch (Exception e) {
            log.error("Error confirming lesson payment", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    // Stripe Webhook handler
    @PostMapping("/webhooks/stripe")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader
    ) {
        Event event;

        try {
            event = stripeService.constructEvent(payload, sigHeader);
        } catch (SignatureVerificationException e) {
            log.warn("Invalid Stripe webhook signature");
            return ResponseEntity.badRequest().body("Invalid signature");
        }

        log.info("Received Stripe webhook: {}", event.getType());

        switch (event.getType()) {
            case "checkout.session.completed" -> handleCheckoutCompleted(event);
            case "payment_intent.succeeded" -> handlePaymentIntentSucceeded(event);
            case "customer.subscription.updated" -> handleSubscriptionUpdated(event);
            case "customer.subscription.deleted" -> handleSubscriptionDeleted(event);
            case "invoice.paid" -> handleInvoicePaid(event);
            case "invoice.payment_failed" -> handlePaymentFailed(event);
            default -> log.debug("Unhandled event type: {}", event.getType());
        }

        return ResponseEntity.ok("Webhook received");
    }

    private void handleCheckoutCompleted(Event event) {
        try {
            Session session = (Session) event.getDataObjectDeserializer()
                    .getObject()
                    .orElseThrow(() -> new RuntimeException("Failed to deserialize session"));

            String mode = session.getMode();
            Map<String, String> metadata = session.getMetadata();

            if ("subscription".equals(mode)) {
                Long userId = Long.parseLong(metadata.get("user_id"));
                SubscriptionPlan plan = SubscriptionPlan.valueOf(metadata.get("plan"));
                String stripeSubId = session.getSubscription();

                subscriptionService.activateSubscription(stripeSubId, userId, plan);
                log.info("Subscription activated for user {} via checkout webhook", userId);
                // Note: Invoice is generated in confirmSubscriptionPayment endpoint
            } else if ("payment".equals(mode) && "ONE_TIME_LESSON".equals(metadata.get("type"))) {
                // Handle one-time lesson payment
                Long studentId = Long.parseLong(metadata.get("user_id"));
                Long teacherId = Long.parseLong(metadata.get("teacher_id"));
                LocalDateTime scheduledAt = LocalDateTime.parse(metadata.get("scheduled_at"));
                int durationMinutes = Integer.parseInt(metadata.get("duration_minutes"));
                String notes = metadata.get("notes");

                BookLessonRequest bookRequest = new BookLessonRequest(
                        teacherId,
                        scheduledAt,
                        durationMinutes,
                        notes,
                        false // Don't use subscription
                );

                // Book the lesson and get the response with lesson ID
                LessonResponse lessonResponse = lessonService.bookLesson(studentId, bookRequest);
                log.info("Lesson booked for student {} with teacher {} after payment", studentId, teacherId);

                // Create Payment entity linked to the lesson for refund tracking
                Lesson lesson = lessonRepository.findById(lessonResponse.id())
                        .orElseThrow(() -> new RuntimeException("Lesson not found after booking"));
                User student = userRepository.findById(studentId)
                        .orElseThrow(() -> new RuntimeException("Student not found"));
                User teacher = userRepository.findById(teacherId)
                        .orElseThrow(() -> new RuntimeException("Teacher not found"));

                Payment payment = new Payment();
                payment.setPayer(student);
                payment.setTeacher(teacher);
                payment.setLesson(lesson);
                payment.setPaymentType(PaymentType.ONE_TIME_LESSON);
                payment.setAmountCents(lesson.getPriceCents());
                payment.setStripePaymentIntentId(session.getPaymentIntent());
                payment.setStatus(PaymentStatus.COMPLETED);
                payment.setProcessedAt(LocalDateTime.now());

                paymentRepository.save(payment);
                log.info("Payment {} created and linked to lesson {} for refund tracking",
                        payment.getId(), lesson.getId());
            }
        } catch (Exception e) {
            log.error("Error handling checkout.session.completed", e);
        }
    }

    /**
     * Handle payment_intent.succeeded event.
     * Note: Invoices are now generated in the confirm endpoints, not via webhooks.
     */
    private void handlePaymentIntentSucceeded(Event event) {
        log.info("PaymentIntent succeeded event received - invoices will be generated via confirm endpoint");
    }

    private void handleSubscriptionUpdated(Event event) {
        log.info("Subscription updated event received");
        // Handle plan changes, status updates, etc.
    }

    private void handleSubscriptionDeleted(Event event) {
        log.info("Subscription deleted event received");
        // Handle subscription cancellation from Stripe side
    }

    private void handleInvoicePaid(Event event) {
        log.info("Invoice paid event received");
        // Handle recurring payment success
    }

    private void handlePaymentFailed(Event event) {
        log.warn("Payment failed event received");
        // Handle failed payment - notify user, retry logic, etc.
    }

    // Admin endpoint to clean up duplicate subscriptions
    @PostMapping("/admin/cleanup-subscriptions")
    public ResponseEntity<Map<String, Object>> cleanupDuplicateSubscriptions() {
        int count = subscriptionService.cleanupDuplicateSubscriptions();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "deactivatedCount", count,
                "message", "Cleaned up " + count + " duplicate subscriptions"
        ));
    }
}
