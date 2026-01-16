package com.chessconnect.controller;

import com.chessconnect.dto.admin.*;
import com.chessconnect.model.Payment;
import com.chessconnect.model.Subscription;
import com.chessconnect.service.AdminService;
import com.chessconnect.service.StripeService;
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

    public AdminController(AdminService adminService, StripeService stripeService) {
        this.adminService = adminService;
        this.stripeService = stripeService;
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

    // ============= ACCOUNTING =============

    /**
     * Get revenue/commission overview
     */
    @GetMapping("/accounting/revenue")
    public ResponseEntity<AccountingResponse> getAccountingOverview() {
        return ResponseEntity.ok(adminService.getAccountingOverview());
    }

    /**
     * Get all teacher balances
     */
    @GetMapping("/accounting/teachers")
    public ResponseEntity<List<TeacherBalanceListResponse>> getTeacherBalances() {
        return ResponseEntity.ok(adminService.getTeacherBalances());
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
}
