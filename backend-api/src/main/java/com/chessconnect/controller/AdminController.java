package com.chessconnect.controller;

import com.chessconnect.dto.admin.*;
import com.chessconnect.dto.lesson.LessonResponse;
import com.chessconnect.model.Payment;
import com.chessconnect.model.Subscription;
import com.chessconnect.service.AdminService;
import com.chessconnect.service.AnalyticsService;
import com.chessconnect.service.StripeService;
import com.chessconnect.service.WalletService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final StripeService stripeService;
    private final AnalyticsService analyticsService;
    private final WalletService walletService;

    public AdminController(AdminService adminService, StripeService stripeService, AnalyticsService analyticsService, WalletService walletService) {
        this.adminService = adminService;
        this.stripeService = stripeService;
        this.analyticsService = analyticsService;
        this.walletService = walletService;
    }

    // ============= USER MANAGEMENT =============

    /**
     * Get paginated list of users with optional role filter
     */
    @GetMapping("/users")
    public ResponseEntity<Page<UserListResponse>> getUsers(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) String role
    ) {
        return ResponseEntity.ok(adminService.getUsers(pageable, role));
    }

    /**
     * Get user details by ID
     */
    @GetMapping("/users/{id}")
    public ResponseEntity<UserListResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getUserById(id));
    }

    /**
     * Suspend a user
     */
    @PatchMapping("/users/{id}/suspend")
    public ResponseEntity<?> suspendUser(
            @PathVariable Long id,
            @RequestBody Map<String, String> request
    ) {
        String reason = request.getOrDefault("reason", "No reason provided");
        adminService.suspendUser(id, reason);
        return ResponseEntity.ok(Map.of("message", "User suspended successfully"));
    }

    /**
     * Reactivate a user
     */
    @PatchMapping("/users/{id}/activate")
    public ResponseEntity<?> activateUser(@PathVariable Long id) {
        adminService.activateUser(id);
        return ResponseEntity.ok(Map.of("message", "User reactivated successfully"));
    }

    /**
     * Delete a user (RGPD compliance).
     * Automatically handles wallet balance by recording ADMIN_REFUND transaction.
     * Returns refund amount for manual bank transfer if applicable.
     */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            // Check and refund wallet balance first
            int refundedAmount = walletService.adminRefundWallet(id, "Suppression compte par admin");

            // Delete the user
            adminService.deleteUser(id);

            if (refundedAmount > 0) {
                String formattedAmount = String.format("%.2f", refundedAmount / 100.0);
                return ResponseEntity.ok(Map.of(
                    "message", "Utilisateur supprime. " + formattedAmount + " EUR a rembourser manuellement.",
                    "refundedAmountCents", refundedAmount,
                    "requiresManualRefund", true
                ));
            }

            return ResponseEntity.ok(Map.of(
                "message", "Utilisateur supprime avec succes",
                "refundedAmountCents", 0,
                "requiresManualRefund", false
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Erreur lors de la suppression: " + e.getMessage()));
        }
    }

    /**
     * Refund user's wallet balance (for manual bank transfer before account deletion).
     * Clears the wallet balance and records the transaction.
     */
    @PostMapping("/users/{id}/refund-wallet")
    public ResponseEntity<?> refundUserWallet(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> request
    ) {
        String reason = request != null ? request.getOrDefault("reason", "") : "";
        int refundedAmount = walletService.adminRefundWallet(id, reason);

        if (refundedAmount == 0) {
            return ResponseEntity.ok(Map.of(
                "message", "Aucun solde a rembourser",
                "refundedAmountCents", 0
            ));
        }

        String formattedAmount = String.format("%.2f", refundedAmount / 100.0);
        return ResponseEntity.ok(Map.of(
            "message", "Portefeuille vide. " + formattedAmount + " EUR a rembourser manuellement.",
            "refundedAmountCents", refundedAmount
        ));
    }

    /**
     * Get user's wallet balance (for admin to check before refund).
     */
    @GetMapping("/users/{id}/wallet")
    public ResponseEntity<?> getUserWallet(@PathVariable Long id) {
        int balance = walletService.getBalance(id);
        return ResponseEntity.ok(Map.of(
            "balanceCents", balance,
            "balanceFormatted", String.format("%.2f EUR", balance / 100.0)
        ));
    }

    // ============= LESSONS =============

    /**
     * Get upcoming lessons (PENDING or CONFIRMED)
     */
    @GetMapping("/lessons/upcoming")
    public ResponseEntity<List<LessonResponse>> getUpcomingLessons() {
        return ResponseEntity.ok(adminService.getUpcomingLessons());
    }

    /**
     * Get completed lessons
     */
    @GetMapping("/lessons/completed")
    public ResponseEntity<List<LessonResponse>> getCompletedLessons() {
        return ResponseEntity.ok(adminService.getCompletedLessons());
    }

    /**
     * Get all past lessons (COMPLETED + CANCELLED) - for admin history/investigation
     */
    @GetMapping("/lessons/history")
    public ResponseEntity<List<LessonResponse>> getPastLessons() {
        return ResponseEntity.ok(adminService.getPastLessons());
    }

    /**
     * Get ALL lessons (all statuses) - complete admin overview
     */
    @GetMapping("/lessons/all")
    public ResponseEntity<List<LessonResponse>> getAllLessons() {
        return ResponseEntity.ok(adminService.getAllLessons());
    }

    // ============= ACCOUNTING =============

    /**
     * Get revenue/commission overview
     */
    @GetMapping("/accounting/revenue")
    public ResponseEntity<AccountingResponse> getAccountingOverview() {
        return ResponseEntity.ok(adminService.getAccountingOverview());
    }

    /**
     * Get all teacher balances with banking info and payout status
     */
    @GetMapping("/accounting/teachers")
    public ResponseEntity<List<TeacherBalanceListResponse>> getTeacherBalances() {
        return ResponseEntity.ok(adminService.getTeacherBalances());
    }

    /**
     * Mark a teacher as paid - transfer a custom amount.
     * Performs a real Stripe Connect transfer to the teacher's account.
     */
    @PostMapping("/accounting/teachers/{teacherId}/pay")
    public ResponseEntity<?> markTeacherPaid(
            @PathVariable Long teacherId,
            @RequestBody Map<String, Object> request
    ) {
        try {
            String yearMonth = request.getOrDefault("yearMonth", java.time.YearMonth.now().toString()).toString();
            String paymentReference = request.getOrDefault("paymentReference", "").toString();
            String notes = request.getOrDefault("notes", "").toString();
            Integer amountCents = request.get("amountCents") != null
                    ? ((Number) request.get("amountCents")).intValue()
                    : null;

            var result = adminService.markTeacherPaid(teacherId, yearMonth, paymentReference, notes, amountCents);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Transfert effectue avec succes",
                    "amountCents", result.amountCents(),
                    "stripeTransferId", result.stripeTransferId() != null ? result.stripeTransferId() : "",
                    "lessonsCount", result.lessonsCount()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Get payment history
     */
    @GetMapping("/accounting/payments")
    public ResponseEntity<Page<Payment>> getPayments(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(adminService.getPayments(pageable));
    }

    // ============= SUBSCRIPTIONS =============

    /**
     * Get all subscriptions
     */
    @GetMapping("/subscriptions")
    public ResponseEntity<Page<Subscription>> getSubscriptions(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(adminService.getSubscriptions(pageable));
    }

    /**
     * Cancel a subscription
     */
    @PostMapping("/subscriptions/{id}/cancel")
    public ResponseEntity<?> cancelSubscription(
            @PathVariable Long id,
            @RequestBody Map<String, String> request
    ) {
        String reason = request.getOrDefault("reason", "Cancelled by admin");
        adminService.cancelSubscription(id, reason);
        return ResponseEntity.ok(Map.of("message", "Subscription cancelled successfully"));
    }

    // ============= PAYMENTS =============

    /**
     * Refund a payment
     */
    @PostMapping("/payments/{paymentIntentId}/refund")
    public ResponseEntity<?> refundPayment(
            @PathVariable String paymentIntentId,
            @RequestBody Map<String, Object> request
    ) {
        int refundPercentage = (Integer) request.getOrDefault("percentage", 100);
        String reason = (String) request.getOrDefault("reason", "Refunded by admin");

        try {
            // Note: This is a simplified implementation - in production you'd want more validation
            stripeService.createPartialRefund(paymentIntentId, 0, refundPercentage, reason);
            return ResponseEntity.ok(Map.of("message", "Refund processed successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Refund failed: " + e.getMessage()));
        }
    }

    // ============= STATS =============

    /**
     * Get admin dashboard statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<AdminStatsResponse> getStats() {
        return ResponseEntity.ok(adminService.getStats());
    }

    // ============= ANALYTICS =============

    /**
     * Get analytics data for charts.
     * @param period "day" (last 7 days), "week" (last 4 weeks), "month" (last 12 months)
     */
    @GetMapping("/analytics")
    public ResponseEntity<AnalyticsResponse> getAnalytics(
            @RequestParam(defaultValue = "day") String period
    ) {
        return ResponseEntity.ok(analyticsService.getAnalytics(period));
    }
}
