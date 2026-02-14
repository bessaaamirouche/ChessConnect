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
import com.chessconnect.service.GroupLessonService;
import com.chessconnect.service.InvoiceService;
import com.chessconnect.service.LessonService;
import com.chessconnect.service.PromoCodeService;
import com.chessconnect.service.StripeService;
import com.chessconnect.service.SubscriptionService;
import com.chessconnect.dto.promo.ValidatePromoCodeResponse;
import com.chessconnect.model.enums.DiscountType;

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
    private final GroupLessonService groupLessonService;
    private final PromoCodeService promoCodeService;

    public PaymentController(
            StripeService stripeService,
            SubscriptionService subscriptionService,
            PaymentRepository paymentRepository,
            UserRepository userRepository,
            LessonRepository lessonRepository,
            LessonService lessonService,
            InvoiceService invoiceService,
            GroupLessonService groupLessonService,
            PromoCodeService promoCodeService
    ) {
        this.stripeService = stripeService;
        this.subscriptionService = subscriptionService;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.lessonRepository = lessonRepository;
        this.lessonService = lessonService;
        this.invoiceService = invoiceService;
        this.groupLessonService = groupLessonService;
        this.promoCodeService = promoCodeService;
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

            int originalAmountCents = teacher.getHourlyRateCents();
            int amountCents = originalAmountCents;
            // Sanitize teacher name for Stripe - remove special characters
            String sanitizedName = teacher.getFullName().replaceAll("[^a-zA-ZÀ-ÿ\\s\\-']", "").trim();
            if (sanitizedName.isEmpty()) {
                sanitizedName = "Coach";
            }
            String description = String.format("Cours d'échecs avec %s", sanitizedName);

            // Promo code handling
            String promoCode = request.getPromoCode();
            String promoMetadata = null;
            if (promoCode != null && !promoCode.isBlank()) {
                ValidatePromoCodeResponse validation = promoCodeService.validateCode(promoCode, userDetails.getId(), amountCents);
                if (!validation.valid()) {
                    return ResponseEntity.badRequest().body(null);
                }
                if (validation.discountType() == DiscountType.STUDENT_DISCOUNT) {
                    amountCents = validation.finalPriceCents();
                }
                promoMetadata = promoCode;
            }

            Session session = stripeService.createLessonPaymentSession(
                    student,
                    teacher.getId(),
                    amountCents,
                    description,
                    request.getScheduledAt().toString(),
                    request.getDurationMinutes(),
                    request.getNotes(),
                    request.isEmbedded(),
                    request.getCourseId(),
                    promoMetadata,
                    originalAmountCents
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

    // Check if user is eligible for 14-day free trial (with Stripe - requires card)
    @GetMapping("/subscription/trial-eligible")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Map<String, Boolean>> checkTrialEligibility(
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        boolean eligible = subscriptionService.isEligibleForTrial(userDetails.getId());
        return ResponseEntity.ok(Map.of("eligible", eligible));
    }

    // Get free trial status (without card)
    @GetMapping("/subscription/free-trial")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Map<String, Object>> getFreeTrialStatus(
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        Map<String, Object> status = subscriptionService.getTrialStatus(userDetails.getId());
        return ResponseEntity.ok(status);
    }

    // Start 14-day free premium trial (no card required)
    @PostMapping("/subscription/free-trial/start")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Map<String, Object>> startFreeTrial(
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        try {
            subscriptionService.startFreeTrial(userDetails.getId());
            Map<String, Object> status = subscriptionService.getTrialStatus(userDetails.getId());
            status.put("success", true);
            status.put("message", "Essai gratuit de 14 jours activé !");
            return ResponseEntity.ok(status);
        } catch (RuntimeException e) {
            log.error("Error starting free trial for user {}", userDetails.getId(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
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
            SubscriptionPlan plan;
            try {
                plan = SubscriptionPlan.valueOf(planName);
            } catch (IllegalArgumentException e) {
                log.error("Invalid subscription plan in Stripe metadata: {}", planName);
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Plan d'abonnement invalide"
                ));
            }
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
            String notes = metadata.get("notes");

            // Detect group lesson join sessions (notes starts with "GROUP_JOIN:")
            if (notes != null && notes.startsWith("GROUP_JOIN:")) {
                String groupToken = notes.substring("GROUP_JOIN:".length()).trim();
                log.info("Detected group join payment for user {} with token {}", userId, groupToken);
                try {
                    var groupResponse = groupLessonService.joinAfterStripePayment(userId, groupToken);
                    return ResponseEntity.ok(Map.of(
                            "success", true,
                            "lessonId", groupResponse.lesson().id(),
                            "message", "Vous avez rejoint le cours !"
                    ));
                } catch (IllegalArgumentException e) {
                    String msg = e.getMessage();
                    if (msg != null && (msg.contains("already a participant") || msg.contains("already full"))) {
                        // Already joined (likely via embedded checkout onComplete callback)
                        return ResponseEntity.ok(Map.of("success", true, "message", "Vous avez rejoint le cours !"));
                    }
                    throw e;
                }
            }

            // Detect group lesson create sessions (notes starts with "GROUP_CREATE:")
            if (notes != null && notes.startsWith("GROUP_CREATE:")) {
                log.info("Detected group create payment for user {} — delegating to group-lessons/create/confirm", userId);
                // Redirect logic handled by GroupLessonController.confirmCreatePayment
                // but also support it here for return_url fallback
                int targetGroupSize = Integer.parseInt(notes.substring("GROUP_CREATE:".length()).trim());
                Long teacherId = Long.parseLong(metadata.get("teacher_id"));
                LocalDateTime scheduledAt = LocalDateTime.parse(metadata.get("scheduled_at"));
                int durationMinutes = Integer.parseInt(metadata.get("duration_minutes"));
                String courseIdStr = metadata.get("course_id");
                Long courseId = (courseIdStr != null && !courseIdStr.isEmpty()) ? Long.parseLong(courseIdStr) : null;

                try {
                    var request = new com.chessconnect.dto.group.BookGroupLessonRequest(
                            teacherId, scheduledAt, durationMinutes, null, targetGroupSize, courseId
                    );
                    var groupResponse = groupLessonService.createGroupLesson(userId, request);

                    // Create payment record
                    User student = userRepository.findById(userId).orElseThrow();
                    User teacher = userRepository.findById(teacherId).orElseThrow();
                    Lesson lesson = lessonRepository.findById(groupResponse.lesson().id()).orElseThrow();
                    int pricePerPerson = com.chessconnect.service.GroupPricingCalculator.calculateParticipantPrice(
                            teacher.getHourlyRateCents(), targetGroupSize);
                    String paymentIntentId = session.getPaymentIntent();

                    Payment payment = new Payment();
                    payment.setPayer(student);
                    payment.setTeacher(teacher);
                    payment.setLesson(lesson);
                    payment.setPaymentType(PaymentType.ONE_TIME_LESSON);
                    payment.setAmountCents(pricePerPerson);
                    payment.setStripePaymentIntentId(paymentIntentId);
                    payment.setStatus(PaymentStatus.COMPLETED);
                    payment.setProcessedAt(LocalDateTime.now());
                    paymentRepository.save(payment);

                    if (paymentIntentId != null && pricePerPerson > 0) {
                        invoiceService.generateInvoicesForPayment(
                                paymentIntentId, userId, teacherId, lesson.getId(), pricePerPerson, false
                        );
                    }

                    return ResponseEntity.ok(Map.of(
                            "success", true,
                            "lessonId", groupResponse.lesson().id(),
                            "invitationToken", groupResponse.invitationToken(),
                            "message", "Cours en groupe cree avec succes"
                    ));
                } catch (IllegalArgumentException e) {
                    throw e;
                }
            }

            Long teacherId = Long.parseLong(metadata.get("teacher_id"));
            LocalDateTime scheduledAt = LocalDateTime.parse(metadata.get("scheduled_at"));
            int durationMinutes = Integer.parseInt(metadata.get("duration_minutes"));
            String courseIdStr = metadata.get("course_id");
            Long courseId = (courseIdStr != null && !courseIdStr.isEmpty()) ? Long.parseLong(courseIdStr) : null;

            // Book the lesson
            BookLessonRequest bookRequest = new BookLessonRequest(
                    teacherId,
                    scheduledAt,
                    durationMinutes,
                    notes,
                    false, // Don't use subscription - this is a paid lesson
                    courseId
            );

            var lessonResponse = lessonService.bookLesson(userId, bookRequest);
            log.info("Lesson {} booked for student {} after payment confirmation", lessonResponse.id(), userId);

            // Create Payment record for refund tracking
            Lesson lesson = lessonRepository.findById(lessonResponse.id())
                    .orElseThrow(() -> new RuntimeException("Lesson not found after booking"));
            User student = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Student not found"));
            User teacher = userRepository.findById(teacherId)
                    .orElseThrow(() -> new RuntimeException("Teacher not found"));

            String paymentIntentId = session.getPaymentIntent();
            int amountCents = session.getAmountTotal() != null ? session.getAmountTotal().intValue() : lesson.getPriceCents();

            // Read promo code metadata
            String promoCodeStr = metadata.get("promo_code");
            String originalAmountStr = metadata.get("original_amount_cents");
            boolean promoApplied = promoCodeStr != null && !promoCodeStr.isBlank();
            int originalAmountCents = (originalAmountStr != null) ? Integer.parseInt(originalAmountStr) : amountCents;

            Payment payment = new Payment();
            payment.setPayer(student);
            payment.setTeacher(teacher);
            payment.setLesson(lesson);
            payment.setPaymentType(PaymentType.ONE_TIME_LESSON);
            payment.setStripePaymentIntentId(paymentIntentId);
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setProcessedAt(LocalDateTime.now());

            if (promoApplied) {
                ValidatePromoCodeResponse validation = promoCodeService.validateCode(promoCodeStr, userId, originalAmountCents);
                if (validation.valid() && validation.discountType() == DiscountType.COMMISSION_REDUCTION) {
                    int standardCommission = (amountCents * 10) / 100;
                    int reducedCommission = (int) (standardCommission * (100.0 - validation.discountPercent()) / 100.0);
                    int teacherPayout = amountCents - reducedCommission;
                    payment.setAmountWithPromo(amountCents, reducedCommission, teacherPayout);
                } else {
                    payment.setAmountCents(amountCents);
                }
                payment.setOriginalAmountCents(originalAmountCents);
                payment.setDiscountAmountCents(originalAmountCents - amountCents);
            } else {
                payment.setAmountCents(amountCents);
            }
            paymentRepository.save(payment);
            log.info("Payment {} created and linked to lesson {} for refund tracking", payment.getId(), lesson.getId());

            // Record promo code usage and referral earning
            if (promoApplied) {
                try {
                    promoCodeService.applyPromoCode(promoCodeStr, userId, lessonResponse.id(), payment.getId(), originalAmountCents);
                    promoCodeService.recordReferralEarning(userId, lessonResponse.id(), amountCents, payment.getCommissionCents());
                } catch (Exception e) {
                    log.error("Error recording promo code usage", e);
                }
            } else {
                promoCodeService.recordReferralEarning(userId, lessonResponse.id(), amountCents, payment.getCommissionCents());
            }

            // Generate invoices
            if (paymentIntentId != null && amountCents > 0) {
                invoiceService.generateInvoicesForPayment(
                        paymentIntentId,
                        userId,
                        teacherId,
                        lessonResponse.id(),
                        amountCents,
                        promoApplied
                );
                log.info("Lesson invoices generated for lesson {}", lessonResponse.id());
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "lessonId", lessonResponse.id(),
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
                String planStr = metadata.get("plan");
                if (planStr == null) {
                    log.error("Missing plan in subscription webhook metadata");
                    return;
                }
                SubscriptionPlan plan;
                try {
                    plan = SubscriptionPlan.valueOf(planStr);
                } catch (IllegalArgumentException e) {
                    log.error("Invalid subscription plan in webhook metadata: {}", planStr);
                    return;
                }
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
                String courseIdStr = metadata.get("course_id");
                Long courseId = (courseIdStr != null && !courseIdStr.isEmpty()) ? Long.parseLong(courseIdStr) : null;

                BookLessonRequest bookRequest = new BookLessonRequest(
                        teacherId,
                        scheduledAt,
                        durationMinutes,
                        notes,
                        false, // Don't use subscription
                        courseId
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
