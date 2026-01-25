package com.chessconnect.controller;

import com.chessconnect.dto.lesson.BookLessonRequest;
import com.chessconnect.dto.lesson.LessonResponse;
import com.chessconnect.dto.payment.CheckoutSessionResponse;
import com.chessconnect.dto.wallet.BookWithCreditRequest;
import com.chessconnect.dto.wallet.CreditTransactionResponse;
import com.chessconnect.dto.wallet.TopUpRequest;
import com.chessconnect.dto.wallet.WalletResponse;
import com.chessconnect.model.Lesson;
import com.chessconnect.model.Payment;
import com.chessconnect.model.User;
import com.chessconnect.model.enums.PaymentStatus;
import com.chessconnect.model.enums.PaymentType;
import com.chessconnect.repository.LessonRepository;
import com.chessconnect.repository.PaymentRepository;
import com.chessconnect.repository.UserRepository;
import com.chessconnect.security.UserDetailsImpl;
import com.chessconnect.service.InvoiceService;
import com.chessconnect.service.LessonService;
import com.chessconnect.service.StripeService;
import com.chessconnect.service.WalletService;
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
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/wallet")
public class WalletController {

    private static final Logger log = LoggerFactory.getLogger(WalletController.class);

    private final WalletService walletService;
    private final LessonService lessonService;
    private final LessonRepository lessonRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final StripeService stripeService;
    private final InvoiceService invoiceService;

    public WalletController(
            WalletService walletService,
            LessonService lessonService,
            LessonRepository lessonRepository,
            PaymentRepository paymentRepository,
            UserRepository userRepository,
            StripeService stripeService,
            InvoiceService invoiceService
    ) {
        this.walletService = walletService;
        this.lessonService = lessonService;
        this.lessonRepository = lessonRepository;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.stripeService = stripeService;
        this.invoiceService = invoiceService;
    }

    /**
     * Get current user's wallet information.
     */
    @GetMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<WalletResponse> getWallet(
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        WalletResponse wallet = walletService.getWallet(userDetails.getId());
        return ResponseEntity.ok(wallet);
    }

    /**
     * Get current user's credit balance.
     */
    @GetMapping("/balance")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Map<String, Integer>> getBalance(
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        int balance = walletService.getBalance(userDetails.getId());
        return ResponseEntity.ok(Map.of("balanceCents", balance));
    }

    /**
     * Get transaction history.
     */
    @GetMapping("/transactions")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<CreditTransactionResponse>> getTransactions(
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        List<CreditTransactionResponse> transactions = walletService.getTransactions(userDetails.getId());
        return ResponseEntity.ok(transactions);
    }

    /**
     * Create a Stripe checkout session for topping up credit.
     */
    @PostMapping("/topup")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<CheckoutSessionResponse> createTopUpSession(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody TopUpRequest request
    ) {
        try {
            Session session = walletService.createTopUpSession(
                    userDetails.getId(),
                    request.amountCents(),
                    request.embedded()
            );

            CheckoutSessionResponse.CheckoutSessionResponseBuilder responseBuilder = CheckoutSessionResponse.builder()
                    .sessionId(session.getId())
                    .publishableKey(stripeService.getPublishableKey());

            if (request.embedded()) {
                responseBuilder.clientSecret(session.getClientSecret());
            } else {
                responseBuilder.url(session.getUrl());
            }

            return ResponseEntity.ok(responseBuilder.build());
        } catch (StripeException e) {
            log.error("Stripe error creating top-up session", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        } catch (RuntimeException e) {
            log.error("Error creating top-up session", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Confirm top-up after Stripe payment.
     */
    @PostMapping("/topup/confirm")
    public ResponseEntity<Map<String, Object>> confirmTopUp(
            @RequestParam String sessionId
    ) {
        try {
            Session session = stripeService.retrieveSession(sessionId);

            // Verify payment is complete
            if (!"paid".equals(session.getPaymentStatus())) {
                log.warn("Top-up payment not completed for session {}", sessionId);
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Le paiement n'a pas été effectué"
                ));
            }

            // Get metadata from session
            Map<String, String> metadata = session.getMetadata();
            String type = metadata.get("type");
            String userIdStr = metadata.get("user_id");
            String amountStr = metadata.get("amount_cents");

            if (!"CREDIT_TOPUP".equals(type) || userIdStr == null || amountStr == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Session invalide"
                ));
            }

            Long userId = Long.parseLong(userIdStr);
            int amountCents = Integer.parseInt(amountStr);
            String paymentIntentId = session.getPaymentIntent();

            // Confirm the top-up
            WalletResponse wallet = walletService.confirmTopUp(userId, amountCents, paymentIntentId);

            // Generate invoice for top-up
            if (paymentIntentId != null) {
                invoiceService.generateTopUpInvoice(userId, amountCents, paymentIntentId);
                log.info("Top-up invoice generated for user {}", userId);
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "balanceCents", wallet.balanceCents(),
                    "message", "Crédit ajouté avec succès"
            ));

        } catch (StripeException e) {
            log.error("Stripe error confirming top-up", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                    "success", false,
                    "error", "Erreur de vérification du paiement"
            ));
        } catch (Exception e) {
            log.error("Error confirming top-up", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Book a lesson using credit.
     */
    @PostMapping("/book-with-credit")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Map<String, Object>> bookWithCredit(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody BookWithCreditRequest request
    ) {
        try {
            Long userId = userDetails.getId();

            // Get teacher's hourly rate
            User teacher = userRepository.findById(request.teacherId())
                    .orElseThrow(() -> new RuntimeException("Teacher not found"));
            int lessonPrice = teacher.getHourlyRateCents();

            // Check if user has enough credit
            if (!walletService.hasEnoughCredit(userId, lessonPrice)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Crédit insuffisant. Solde actuel: " +
                                String.format("%.2f€", walletService.getBalance(userId) / 100.0)
                ));
            }

            // Book the lesson
            BookLessonRequest bookRequest = new BookLessonRequest(
                    request.teacherId(),
                    request.scheduledAt(),
                    request.durationMinutes(),
                    request.notes(),
                    false
            );

            LessonResponse lessonResponse = lessonService.bookLesson(userId, bookRequest);

            // Get the lesson entity
            Lesson lesson = lessonRepository.findById(lessonResponse.id())
                    .orElseThrow(() -> new RuntimeException("Lesson not found after booking"));

            // Deduct credit
            walletService.deductCreditForLesson(userId, lesson, lessonPrice);

            // Create payment record for tracking
            User student = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Student not found"));

            Payment payment = new Payment();
            payment.setPayer(student);
            payment.setTeacher(teacher);
            payment.setLesson(lesson);
            payment.setPaymentType(PaymentType.LESSON_FROM_CREDIT);
            payment.setAmountCents(lessonPrice);
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setProcessedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            // Generate invoice
            invoiceService.generateInvoicesForCreditPayment(
                    userId,
                    request.teacherId(),
                    lesson.getId(),
                    lessonPrice
            );

            log.info("Lesson {} booked with credit for user {}", lessonResponse.id(), userId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "lessonId", lessonResponse.id(),
                    "remainingBalanceCents", walletService.getBalance(userId),
                    "message", "Cours réservé avec succès"
            ));

        } catch (RuntimeException e) {
            log.error("Error booking lesson with credit", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }
}
