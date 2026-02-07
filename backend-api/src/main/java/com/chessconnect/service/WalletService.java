package com.chessconnect.service;

import com.chessconnect.dto.wallet.CreditTransactionResponse;
import com.chessconnect.dto.wallet.WalletResponse;
import com.chessconnect.model.CreditTransaction;
import com.chessconnect.model.Lesson;
import com.chessconnect.model.StudentWallet;
import com.chessconnect.model.User;
import com.chessconnect.model.enums.CreditTransactionType;
import com.chessconnect.model.enums.UserRole;
import com.chessconnect.repository.CreditTransactionRepository;
import com.chessconnect.repository.StudentWalletRepository;
import com.chessconnect.repository.UserRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WalletService {

    private static final Logger log = LoggerFactory.getLogger(WalletService.class);

    private final StudentWalletRepository walletRepository;
    private final CreditTransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final StripeService stripeService;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    public WalletService(
            StudentWalletRepository walletRepository,
            CreditTransactionRepository transactionRepository,
            UserRepository userRepository,
            StripeService stripeService
    ) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.stripeService = stripeService;
    }

    /**
     * Get or create wallet for a user.
     */
    public StudentWallet getOrCreateWallet(Long userId) {
        return walletRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new IllegalArgumentException("User not found"));

                    if (user.getRole() != UserRole.STUDENT) {
                        throw new IllegalArgumentException("Only students can have a wallet");
                    }

                    StudentWallet wallet = new StudentWallet();
                    wallet.setUser(user);
                    return walletRepository.save(wallet);
                });
    }

    /**
     * Get wallet information for a user.
     */
    public WalletResponse getWallet(Long userId) {
        return walletRepository.findByUserId(userId)
                .map(WalletResponse::from)
                .orElse(WalletResponse.empty());
    }

    /**
     * Get credit balance for a user.
     */
    public int getBalance(Long userId) {
        return walletRepository.findByUserId(userId)
                .map(StudentWallet::getBalanceCents)
                .orElse(0);
    }

    /**
     * Check if user has enough credit.
     */
    public boolean hasEnoughCredit(Long userId, int amountCents) {
        return getBalance(userId) >= amountCents;
    }

    /**
     * Get transaction history for a user.
     */
    public List<CreditTransactionResponse> getTransactions(Long userId) {
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(CreditTransactionResponse::from)
                .toList();
    }

    /**
     * Create a Stripe checkout session for topping up credit.
     */
    public Session createTopUpSession(Long userId, int amountCents, boolean embedded) throws StripeException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getRole() != UserRole.STUDENT) {
            throw new IllegalArgumentException("Only students can top up credit");
        }

        SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                .setCustomerEmail(user.getEmail())
                .putMetadata("user_id", user.getId().toString())
                .putMetadata("type", "CREDIT_TOPUP")
                .putMetadata("amount_cents", String.valueOf(amountCents))
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("eur")
                                                .setUnitAmount((long) amountCents)
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName("Recharge crédit")
                                                                .setDescription("Crédit pour cours d'échecs - ChessConnect")
                                                                .build()
                                                )
                                                .build()
                                )
                                .setQuantity(1L)
                                .build()
                );

        if (embedded) {
            paramsBuilder.setUiMode(SessionCreateParams.UiMode.EMBEDDED);
            paramsBuilder.setReturnUrl(frontendUrl + "/wallet?topup=success&session_id={CHECKOUT_SESSION_ID}");
        } else {
            paramsBuilder.setSuccessUrl(frontendUrl + "/wallet?topup=success&session_id={CHECKOUT_SESSION_ID}");
            paramsBuilder.setCancelUrl(frontendUrl + "/wallet?topup=cancel");
        }

        Session session = Session.create(paramsBuilder.build());
        log.info("Created {} top-up session {} for user {} - Amount: {} cents",
                embedded ? "embedded" : "hosted", session.getId(), userId, amountCents);
        return session;
    }

    /**
     * Confirm a top-up after Stripe payment.
     */
    @Transactional
    public WalletResponse confirmTopUp(Long userId, int amountCents, String stripePaymentIntentId) {
        // Idempotency check: prevent replay of the same payment
        if (stripePaymentIntentId != null && transactionRepository.existsByStripePaymentIntentId(stripePaymentIntentId)) {
            log.warn("Duplicate top-up attempt for paymentIntent {}", stripePaymentIntentId);
            return getWallet(userId);
        }

        StudentWallet wallet = getOrCreateWallet(userId);

        wallet.addCredit(amountCents);
        walletRepository.save(wallet);

        // Record transaction
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        CreditTransaction transaction = new CreditTransaction();
        transaction.setUser(user);
        transaction.setTransactionType(CreditTransactionType.TOPUP);
        transaction.setAmountCents(amountCents);
        transaction.setStripePaymentIntentId(stripePaymentIntentId);
        transaction.setDescription("Recharge de crédit");
        transactionRepository.save(transaction);

        log.info("Top-up confirmed for user {}: {} cents. New balance: {} cents",
                userId, amountCents, wallet.getBalanceCents());

        return WalletResponse.from(wallet);
    }

    /**
     * Atomically check balance and deduct credit under pessimistic lock.
     * Prevents race conditions (TOCTOU) on concurrent booking requests.
     */
    @Transactional
    public void checkAndDeductCredit(Long userId, int amountCents) {
        StudentWallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseGet(() -> getOrCreateWallet(userId));

        if (!wallet.hasEnoughCredit(amountCents)) {
            throw new IllegalArgumentException("Crédit insuffisant. Solde actuel: " +
                    String.format("%.2f€", wallet.getBalanceCents() / 100.0));
        }

        wallet.deductCredit(amountCents);
        walletRepository.save(wallet);

        log.info("Credit reserved for user {}: {} cents. New balance: {} cents",
                userId, amountCents, wallet.getBalanceCents());
    }

    /**
     * Record the deduction transaction linked to a specific lesson.
     */
    @Transactional
    public void linkDeductionToLesson(Long userId, Lesson lesson, int amountCents) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        CreditTransaction transaction = new CreditTransaction();
        transaction.setUser(user);
        transaction.setLesson(lesson);
        transaction.setTransactionType(CreditTransactionType.LESSON_PAYMENT);
        transaction.setAmountCents(amountCents);
        transaction.setDescription("Cours avec " + lesson.getTeacher().getFullName());
        transactionRepository.save(transaction);
    }

    /**
     * Deduct credit for a lesson payment.
     */
    @Transactional
    public void deductCreditForLesson(Long userId, Lesson lesson, int amountCents) {
        // Use pessimistic lock to prevent race conditions (TOCTOU)
        StudentWallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseGet(() -> getOrCreateWallet(userId));

        if (!wallet.hasEnoughCredit(amountCents)) {
            throw new IllegalArgumentException("Insufficient credit balance");
        }

        wallet.deductCredit(amountCents);
        walletRepository.save(wallet);

        // Record transaction
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        CreditTransaction transaction = new CreditTransaction();
        transaction.setUser(user);
        transaction.setLesson(lesson);
        transaction.setTransactionType(CreditTransactionType.LESSON_PAYMENT);
        transaction.setAmountCents(amountCents);
        transaction.setDescription("Cours avec " + lesson.getTeacher().getFullName());
        transactionRepository.save(transaction);

        log.info("Credit deducted for user {} for lesson {}: {} cents. New balance: {} cents",
                userId, lesson.getId(), amountCents, wallet.getBalanceCents());
    }

    /**
     * Refund credit for a cancelled lesson.
     */
    @Transactional
    public void refundCreditForLesson(Long userId, Lesson lesson, int amountCents, int refundPercentage) {
        int refundAmount = (amountCents * refundPercentage) / 100;

        if (refundAmount <= 0) {
            log.info("No credit refund for lesson {} (0%)", lesson.getId());
            return;
        }

        StudentWallet wallet = getOrCreateWallet(userId);
        wallet.refundCredit(refundAmount);
        walletRepository.save(wallet);

        // Record transaction
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        CreditTransaction transaction = new CreditTransaction();
        transaction.setUser(user);
        transaction.setLesson(lesson);
        transaction.setTransactionType(CreditTransactionType.REFUND);
        transaction.setAmountCents(refundAmount);
        transaction.setDescription(String.format("Remboursement cours annulé (%d%%)", refundPercentage));
        transactionRepository.save(transaction);

        log.info("Credit refunded for user {} for lesson {}: {} cents ({}%). New balance: {} cents",
                userId, lesson.getId(), refundAmount, refundPercentage, wallet.getBalanceCents());
    }

    /**
     * Admin refund - clears user wallet balance for manual refund (e.g., before account deletion).
     * The admin must manually transfer the money to the user via bank transfer.
     * @return the amount that was refunded (in cents)
     */
    @Transactional
    public int adminRefundWallet(Long userId, String reason) {
        StudentWallet wallet = walletRepository.findByUserId(userId).orElse(null);

        if (wallet == null || wallet.getBalanceCents() <= 0) {
            return 0;
        }

        int refundAmount = wallet.getBalanceCents();

        // Record transaction before clearing
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        CreditTransaction transaction = new CreditTransaction();
        transaction.setUser(user);
        transaction.setTransactionType(CreditTransactionType.ADMIN_REFUND);
        transaction.setAmountCents(refundAmount);
        transaction.setDescription("Remboursement admin" + (reason != null && !reason.isBlank() ? ": " + reason : ""));
        transactionRepository.save(transaction);

        // Clear wallet balance
        wallet.setBalanceCents(0);
        walletRepository.save(wallet);

        log.info("Admin refund for user {}: {} cents cleared. Reason: {}", userId, refundAmount, reason);

        return refundAmount;
    }
}
