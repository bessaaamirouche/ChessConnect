package com.chessconnect.controller;

import com.chessconnect.dto.group.*;
import com.chessconnect.dto.payment.CheckoutSessionResponse;
import com.chessconnect.model.GroupInvitation;
import com.chessconnect.model.Lesson;
import com.chessconnect.model.User;
import com.chessconnect.model.Payment;
import com.chessconnect.model.enums.PaymentStatus;
import com.chessconnect.model.enums.PaymentType;
import com.chessconnect.repository.GroupInvitationRepository;
import com.chessconnect.repository.LessonRepository;
import com.chessconnect.repository.PaymentRepository;
import com.chessconnect.repository.UserRepository;
import com.chessconnect.security.UserDetailsImpl;
import com.chessconnect.service.*;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/group-lessons")
public class GroupLessonController {

    private static final Logger log = LoggerFactory.getLogger(GroupLessonController.class);

    private final GroupLessonService groupLessonService;
    private final StripeService stripeService;
    private final WalletService walletService;
    private final InvoiceService invoiceService;
    private final UserRepository userRepository;
    private final GroupInvitationRepository invitationRepository;
    private final LessonRepository lessonRepository;
    private final PaymentRepository paymentRepository;

    public GroupLessonController(
            GroupLessonService groupLessonService,
            StripeService stripeService,
            WalletService walletService,
            InvoiceService invoiceService,
            UserRepository userRepository,
            GroupInvitationRepository invitationRepository,
            LessonRepository lessonRepository,
            PaymentRepository paymentRepository
    ) {
        this.groupLessonService = groupLessonService;
        this.stripeService = stripeService;
        this.walletService = walletService;
        this.invoiceService = invoiceService;
        this.userRepository = userRepository;
        this.invitationRepository = invitationRepository;
        this.lessonRepository = lessonRepository;
        this.paymentRepository = paymentRepository;
    }

    /**
     * Create a group lesson. Creator pays immediately with wallet.
     */
    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Map<String, Object>> create(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody BookGroupLessonRequest request
    ) {
        try {
            Long userId = userDetails.getId();

            // Get teacher to calculate price
            User teacher = userRepository.findById(request.teacherId())
                    .orElseThrow(() -> new RuntimeException("Teacher not found"));

            int pricePerPerson = GroupPricingCalculator.calculateParticipantPrice(
                    teacher.getHourlyRateCents(), request.targetGroupSize());

            // Deduct creator's share
            walletService.checkAndDeductCredit(userId, pricePerPerson);

            // Create the group lesson
            GroupLessonResponse response = groupLessonService.createGroupLesson(userId, request);

            // Link wallet transaction
            Lesson lesson = lessonRepository.findById(response.lesson().id())
                    .orElseThrow(() -> new RuntimeException("Lesson not found"));
            walletService.linkDeductionToLesson(userId, lesson, pricePerPerson);

            // Create payment record
            User student = userRepository.findById(userId).orElseThrow();
            Payment payment = new Payment();
            payment.setPayer(student);
            payment.setTeacher(teacher);
            payment.setLesson(lesson);
            payment.setPaymentType(PaymentType.LESSON_FROM_CREDIT);
            payment.setAmountCents(pricePerPerson);
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setProcessedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            // Generate invoice for creator
            invoiceService.generateInvoicesForCreditPayment(userId, request.teacherId(), lesson.getId(), pricePerPerson);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "lessonId", response.lesson().id(),
                    "invitationToken", response.invitationToken(),
                    "pricePerPersonCents", pricePerPerson,
                    "remainingBalanceCents", walletService.getBalance(userId),
                    "message", "Cours en groupe cree avec succes"
            ));
        } catch (RuntimeException e) {
            log.error("Error creating group lesson", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Get invitation details (public, no auth required).
     */
    @GetMapping("/invitation/{token}")
    public ResponseEntity<GroupInvitationResponse> getInvitation(@PathVariable String token) {
        try {
            GroupInvitationResponse response = groupLessonService.getInvitationDetails(token);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Join a group lesson with wallet credit.
     */
    @PostMapping("/join/credit")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Map<String, Object>> joinWithCredit(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody JoinGroupLessonRequest request
    ) {
        try {
            GroupLessonResponse response = groupLessonService.joinWithCredit(userDetails.getId(), request.token());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "lessonId", response.lesson().id(),
                    "remainingBalanceCents", walletService.getBalance(userDetails.getId()),
                    "message", "Vous avez rejoint le cours !"
            ));
        } catch (RuntimeException e) {
            log.error("Error joining group lesson with credit", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Create Stripe checkout to join a group lesson.
     */
    @PostMapping("/join/checkout")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> joinCheckout(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody JoinGroupLessonRequest request
    ) {
        try {
            GroupInvitation invitation = invitationRepository.findByToken(request.token())
                    .orElseThrow(() -> new RuntimeException("Invitation not found"));

            Lesson lesson = invitation.getLesson();
            int pricePerPerson = GroupPricingCalculator.calculateParticipantPrice(
                    lesson.getPriceCents(), invitation.getMaxParticipants());

            User student = userRepository.findById(userDetails.getId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            String description = String.format("Cours en groupe avec %s", lesson.getTeacher().getDisplayName());

            // Pass group token in notes metadata so confirmJoinPayment can extract it
            String notesWithToken = "GROUP_JOIN:" + request.token();

            Session session = stripeService.createLessonPaymentSession(
                    student,
                    lesson.getTeacher().getId(),
                    pricePerPerson,
                    description,
                    lesson.getScheduledAt().toString(),
                    lesson.getDurationMinutes(),
                    notesWithToken,
                    true, // embedded
                    null
            );

            return ResponseEntity.ok(CheckoutSessionResponse.builder()
                    .sessionId(session.getId())
                    .publishableKey(stripeService.getPublishableKey())
                    .clientSecret(session.getClientSecret())
                    .build());

        } catch (StripeException e) {
            log.error("Stripe error creating group join checkout", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                    "success", false,
                    "error", "Erreur de paiement"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Confirm Stripe payment for joining a group lesson.
     */
    @PostMapping("/join/confirm")
    public ResponseEntity<Map<String, Object>> confirmJoinPayment(@RequestParam String sessionId) {
        try {
            Session session = stripeService.retrieveSession(sessionId);

            if (!"paid".equals(session.getPaymentStatus())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Le paiement n'a pas ete effectue"
                ));
            }

            Map<String, String> metadata = session.getMetadata();
            // user_id is stored by createLessonPaymentSession
            String userIdStr = metadata.get("user_id");
            if (userIdStr == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Identifiant utilisateur introuvable"
                ));
            }
            Long userId = Long.parseLong(userIdStr);

            // Extract group token from notes metadata (format: "GROUP_JOIN:<token>")
            String notes = metadata.get("notes");
            String token = null;
            if (notes != null && notes.startsWith("GROUP_JOIN:")) {
                token = notes.substring("GROUP_JOIN:".length()).trim();
            }

            if (token == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Token d'invitation introuvable"
                ));
            }

            // Use joinAfterStripePayment to avoid double-charging (Stripe already charged)
            GroupLessonResponse response = groupLessonService.joinAfterStripePayment(userId, token);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "lessonId", response.lesson().id(),
                    "message", "Vous avez rejoint le cours !"
            ));

        } catch (StripeException e) {
            log.error("Stripe error confirming group join", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                    "success", false,
                    "error", "Erreur de verification du paiement"
            ));
        } catch (Exception e) {
            log.error("Error confirming group join", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Create Stripe checkout to create a group lesson (alternative to wallet).
     */
    @PostMapping("/checkout")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> createCheckout(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody BookGroupLessonRequest request
    ) {
        try {
            User teacher = userRepository.findById(request.teacherId())
                    .orElseThrow(() -> new RuntimeException("Teacher not found"));

            int pricePerPerson = GroupPricingCalculator.calculateParticipantPrice(
                    teacher.getHourlyRateCents(), request.targetGroupSize());

            User student = userRepository.findById(userDetails.getId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            String sanitizedName = teacher.getFullName().replaceAll("[^a-zA-ZÀ-ÿ\\s\\-']", "").trim();
            if (sanitizedName.isEmpty()) sanitizedName = "Coach";
            String description = String.format("Cours en groupe avec %s", sanitizedName);

            // Encode group info in notes: "GROUP_CREATE:<targetGroupSize>"
            String notesWithGroup = "GROUP_CREATE:" + request.targetGroupSize();

            int durationMinutes = request.durationMinutes() != null ? request.durationMinutes() : 60;

            Session session = stripeService.createLessonPaymentSession(
                    student,
                    teacher.getId(),
                    pricePerPerson,
                    description,
                    request.scheduledAt().toString(),
                    durationMinutes,
                    notesWithGroup,
                    true, // embedded
                    request.courseId()
            );

            return ResponseEntity.ok(CheckoutSessionResponse.builder()
                    .sessionId(session.getId())
                    .publishableKey(stripeService.getPublishableKey())
                    .clientSecret(session.getClientSecret())
                    .build());

        } catch (StripeException e) {
            log.error("Stripe error creating group lesson checkout", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                    "success", false,
                    "error", "Erreur de paiement"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Confirm Stripe payment for creating a group lesson.
     */
    @PostMapping("/create/confirm")
    public ResponseEntity<Map<String, Object>> confirmCreatePayment(@RequestParam String sessionId) {
        try {
            Session session = stripeService.retrieveSession(sessionId);

            if (!"paid".equals(session.getPaymentStatus())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Le paiement n'a pas ete effectue"
                ));
            }

            Map<String, String> metadata = session.getMetadata();
            String userIdStr = metadata.get("user_id");
            if (userIdStr == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Identifiant utilisateur introuvable"
                ));
            }
            Long userId = Long.parseLong(userIdStr);

            // Extract group size from notes "GROUP_CREATE:<size>"
            String notes = metadata.get("notes");
            if (notes == null || !notes.startsWith("GROUP_CREATE:")) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Session invalide pour un cours en groupe"
                ));
            }
            int targetGroupSize = Integer.parseInt(notes.substring("GROUP_CREATE:".length()).trim());

            Long teacherId = Long.parseLong(metadata.get("teacher_id"));
            LocalDateTime scheduledAt = LocalDateTime.parse(metadata.get("scheduled_at"));
            int durationMinutes = Integer.parseInt(metadata.get("duration_minutes"));
            String courseIdStr = metadata.get("course_id");
            Long courseId = (courseIdStr != null && !courseIdStr.isEmpty()) ? Long.parseLong(courseIdStr) : null;

            // Build request and create the group lesson (no wallet deduction — Stripe already charged)
            BookGroupLessonRequest request = new BookGroupLessonRequest(
                    teacherId, scheduledAt, durationMinutes, null, targetGroupSize, courseId
            );
            GroupLessonResponse response = groupLessonService.createGroupLesson(userId, request);

            // Create payment record with Stripe payment intent
            User student = userRepository.findById(userId).orElseThrow();
            User teacher = userRepository.findById(teacherId).orElseThrow();
            Lesson lesson = lessonRepository.findById(response.lesson().id()).orElseThrow();

            int pricePerPerson = GroupPricingCalculator.calculateParticipantPrice(
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

            // Generate invoice
            if (paymentIntentId != null && pricePerPerson > 0) {
                invoiceService.generateInvoicesForPayment(
                        paymentIntentId, userId, teacherId, lesson.getId(), pricePerPerson, false
                );
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "lessonId", response.lesson().id(),
                    "invitationToken", response.invitationToken(),
                    "message", "Cours en groupe cree avec succes"
            ));

        } catch (StripeException e) {
            log.error("Stripe error confirming group create payment", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                    "success", false,
                    "error", "Erreur de verification du paiement"
            ));
        } catch (Exception e) {
            log.error("Error confirming group create payment", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Creator resolves the deadline (PAY_FULL or CANCEL).
     */
    @PostMapping("/{id}/resolve-deadline")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Map<String, Object>> resolveDeadline(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody ResolveDeadlineRequest request
    ) {
        try {
            groupLessonService.resolveDeadline(id, userDetails.getId(), request.choice());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "CANCEL".equals(request.choice())
                            ? "Cours annule et remboursements effectues"
                            : "Cours converti en cours prive"
            ));
        } catch (RuntimeException e) {
            log.error("Error resolving group deadline", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Leave a group lesson (cancel participation).
     */
    @DeleteMapping("/{id}/leave")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Map<String, Object>> leave(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(required = false) String reason
    ) {
        try {
            groupLessonService.cancelParticipant(id, userDetails.getId(), reason);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Vous avez quitte le cours"
            ));
        } catch (RuntimeException e) {
            log.error("Error leaving group lesson", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Get group lesson details.
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GroupLessonResponse> getDetails(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        try {
            GroupLessonResponse response = groupLessonService.getGroupLessonDetails(id, userDetails.getId());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }
}
