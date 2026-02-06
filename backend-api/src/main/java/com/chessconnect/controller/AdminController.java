package com.chessconnect.controller;

import com.chessconnect.dto.admin.*;
import com.chessconnect.dto.lesson.LessonResponse;
import com.chessconnect.model.Availability;
import com.chessconnect.model.Lesson;
import com.chessconnect.model.Payment;
import com.chessconnect.model.Subscription;
import com.chessconnect.model.User;
import com.chessconnect.model.enums.UserRole;
import com.chessconnect.repository.AvailabilityRepository;
import com.chessconnect.repository.LessonRepository;
import com.chessconnect.repository.UserRepository;
import com.chessconnect.service.AdminService;
import com.chessconnect.service.AnalyticsService;
import com.chessconnect.service.EmailService;
import com.chessconnect.service.StripeConnectService;
import com.chessconnect.service.StripeService;
import com.chessconnect.service.ThumbnailService;
import com.chessconnect.service.WalletService;
import com.stripe.exception.StripeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
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
    private final StripeConnectService stripeConnectService;
    private final AnalyticsService analyticsService;
    private final WalletService walletService;
    private final ThumbnailService thumbnailService;
    private final UserRepository userRepository;
    private final LessonRepository lessonRepository;
    private final AvailabilityRepository availabilityRepository;
    private final EmailService emailService;

    @org.springframework.beans.factory.annotation.Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    public AdminController(
            AdminService adminService,
            StripeService stripeService,
            StripeConnectService stripeConnectService,
            AnalyticsService analyticsService,
            WalletService walletService,
            ThumbnailService thumbnailService,
            UserRepository userRepository,
            LessonRepository lessonRepository,
            AvailabilityRepository availabilityRepository,
            EmailService emailService
    ) {
        this.adminService = adminService;
        this.stripeService = stripeService;
        this.stripeConnectService = stripeConnectService;
        this.analyticsService = analyticsService;
        this.walletService = walletService;
        this.thumbnailService = thumbnailService;
        this.userRepository = userRepository;
        this.lessonRepository = lessonRepository;
        this.availabilityRepository = availabilityRepository;
        this.emailService = emailService;
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

    // ============= STRIPE CONNECT =============

    /**
     * Get all coaches with their Stripe Connect status.
     */
    @GetMapping("/stripe-connect/accounts")
    public ResponseEntity<?> getStripeConnectAccounts() {
        try {
            List<User> teachers = userRepository.findByRole(UserRole.TEACHER);
            List<Map<String, Object>> accounts = new java.util.ArrayList<>();

            for (User teacher : teachers) {
                Map<String, Object> accountInfo = new java.util.HashMap<>();
                accountInfo.put("teacherId", teacher.getId());
                accountInfo.put("teacherName", teacher.getFullName());
                accountInfo.put("teacherEmail", teacher.getEmail());
                accountInfo.put("stripeAccountId", teacher.getStripeConnectAccountId());
                accountInfo.put("hasStripeAccount", teacher.getStripeConnectAccountId() != null);

                if (teacher.getStripeConnectAccountId() != null) {
                    try {
                        var details = stripeConnectService.getAccountDetails(teacher.getStripeConnectAccountId());
                        if (details != null) {
                            accountInfo.put("chargesEnabled", details.chargesEnabled());
                            accountInfo.put("payoutsEnabled", details.payoutsEnabled());
                            accountInfo.put("detailsSubmitted", details.detailsSubmitted());
                            accountInfo.put("isReady", details.payoutsEnabled() && details.detailsSubmitted());
                            accountInfo.put("pendingRequirements", details.pendingRequirements());
                            accountInfo.put("stripeEmail", details.email());
                        }
                    } catch (StripeException e) {
                        accountInfo.put("error", "Erreur Stripe: " + e.getMessage());
                    }
                } else {
                    accountInfo.put("isReady", false);
                }

                accounts.add(accountInfo);
            }

            return ResponseEntity.ok(accounts);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Get Express Dashboard link for a specific coach.
     */
    @PostMapping("/stripe-connect/accounts/{teacherId}/dashboard-link")
    public ResponseEntity<?> getExpressDashboardLink(@PathVariable Long teacherId) {
        try {
            User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new IllegalArgumentException("Coach non trouve"));

            if (teacher.getStripeConnectAccountId() == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Ce coach n'a pas de compte Stripe Connect"
                ));
            }

            String dashboardUrl = stripeConnectService.createExpressDashboardLink(teacher.getStripeConnectAccountId());

            return ResponseEntity.ok(Map.of(
                "success", true,
                "dashboardUrl", dashboardUrl
            ));
        } catch (StripeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Erreur Stripe: " + e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Get detailed Stripe account info for a specific coach.
     */
    @GetMapping("/stripe-connect/accounts/{teacherId}")
    public ResponseEntity<?> getStripeAccountDetails(@PathVariable Long teacherId) {
        try {
            User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new IllegalArgumentException("Coach non trouve"));

            if (teacher.getStripeConnectAccountId() == null) {
                return ResponseEntity.ok(Map.of(
                    "hasAccount", false,
                    "teacherName", teacher.getFullName()
                ));
            }

            var details = stripeConnectService.getAccountDetails(teacher.getStripeConnectAccountId());

            return ResponseEntity.ok(Map.of(
                "hasAccount", true,
                "teacherName", teacher.getFullName(),
                "accountId", details.accountId(),
                "email", details.email() != null ? details.email() : "",
                "chargesEnabled", details.chargesEnabled(),
                "payoutsEnabled", details.payoutsEnabled(),
                "detailsSubmitted", details.detailsSubmitted(),
                "isReady", details.payoutsEnabled() && details.detailsSubmitted(),
                "pendingRequirements", details.pendingRequirements() != null ? details.pendingRequirements() : ""
            ));
        } catch (StripeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Erreur Stripe: " + e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    // ============= MESSAGES / NOTES =============

    /**
     * Get all messages/notes from lessons for admin review.
     * Filters: dateFrom, dateTo, teacherId, studentId
     */
    @GetMapping("/messages")
    public ResponseEntity<?> getLessonMessages(
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) Long studentId
    ) {
        try {
            // Parse dates
            LocalDateTime startDate = dateFrom != null ?
                LocalDate.parse(dateFrom).atStartOfDay() :
                LocalDateTime.now().minusMonths(3); // Default: last 3 months

            LocalDateTime endDate = dateTo != null ?
                LocalDate.parse(dateTo).atTime(23, 59, 59) :
                LocalDateTime.now();

            // Get lessons with messages
            List<Lesson> lessons = lessonRepository.findLessonsWithMessages(
                startDate, endDate, teacherId, studentId
            );

            // Map to response
            List<Map<String, Object>> messages = new ArrayList<>();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            for (Lesson lesson : lessons) {
                // Add student notes if present
                if (lesson.getNotes() != null && !lesson.getNotes().isBlank()) {
                    Map<String, Object> noteMessage = new HashMap<>();
                    noteMessage.put("id", lesson.getId());
                    noteMessage.put("type", "NOTE_ETUDIANT");
                    noteMessage.put("from", lesson.getStudent().getFullName());
                    noteMessage.put("fromId", lesson.getStudent().getId());
                    noteMessage.put("fromRole", "STUDENT");
                    noteMessage.put("to", lesson.getTeacher().getFullName());
                    noteMessage.put("toId", lesson.getTeacher().getId());
                    noteMessage.put("toRole", "TEACHER");
                    noteMessage.put("content", lesson.getNotes());
                    noteMessage.put("date", lesson.getCreatedAt().format(formatter));
                    noteMessage.put("lessonDate", lesson.getScheduledAt().format(formatter));
                    noteMessage.put("lessonId", lesson.getId());
                    messages.add(noteMessage);
                }

                // Add teacher observations if present
                if (lesson.getTeacherObservations() != null && !lesson.getTeacherObservations().isBlank()) {
                    Map<String, Object> obsMessage = new HashMap<>();
                    obsMessage.put("id", lesson.getId() * 10 + 1);
                    obsMessage.put("type", "OBSERVATION_COACH");
                    obsMessage.put("from", lesson.getTeacher().getFullName());
                    obsMessage.put("fromId", lesson.getTeacher().getId());
                    obsMessage.put("fromRole", "TEACHER");
                    obsMessage.put("to", lesson.getStudent().getFullName());
                    obsMessage.put("toId", lesson.getStudent().getId());
                    obsMessage.put("toRole", "STUDENT");
                    obsMessage.put("content", lesson.getTeacherObservations());
                    obsMessage.put("date", lesson.getUpdatedAt() != null ? lesson.getUpdatedAt().format(formatter) : lesson.getCreatedAt().format(formatter));
                    obsMessage.put("lessonDate", lesson.getScheduledAt().format(formatter));
                    obsMessage.put("lessonId", lesson.getId());
                    messages.add(obsMessage);
                }

                // Add teacher comment if present
                if (lesson.getTeacherComment() != null && !lesson.getTeacherComment().isBlank()) {
                    Map<String, Object> commentMessage = new HashMap<>();
                    commentMessage.put("id", lesson.getId() * 10 + 2);
                    commentMessage.put("type", "COMMENTAIRE_COACH");
                    commentMessage.put("from", lesson.getTeacher().getFullName());
                    commentMessage.put("fromId", lesson.getTeacher().getId());
                    commentMessage.put("fromRole", "TEACHER");
                    commentMessage.put("to", lesson.getStudent().getFullName());
                    commentMessage.put("toId", lesson.getStudent().getId());
                    commentMessage.put("toRole", "STUDENT");
                    commentMessage.put("content", lesson.getTeacherComment());
                    commentMessage.put("date", lesson.getTeacherCommentAt() != null ? lesson.getTeacherCommentAt().format(formatter) : lesson.getCreatedAt().format(formatter));
                    commentMessage.put("lessonDate", lesson.getScheduledAt().format(formatter));
                    commentMessage.put("lessonId", lesson.getId());
                    messages.add(commentMessage);
                }
            }

            // Sort by date descending
            messages.sort((a, b) -> ((String) b.get("date")).compareTo((String) a.get("date")));

            return ResponseEntity.ok(Map.of(
                "messages", messages,
                "total", messages.size(),
                "filters", Map.of(
                    "dateFrom", dateFrom != null ? dateFrom : "",
                    "dateTo", dateTo != null ? dateTo : "",
                    "teacherId", teacherId != null ? teacherId : "",
                    "studentId", studentId != null ? studentId : ""
                )
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Get list of teachers for filter dropdown.
     */
    @GetMapping("/messages/teachers")
    public ResponseEntity<?> getTeachersForFilter() {
        List<User> teachers = userRepository.findByRole(UserRole.TEACHER);
        return ResponseEntity.ok(teachers.stream()
            .map(t -> Map.of("id", t.getId(), "name", t.getFullName()))
            .toList()
        );
    }

    /**
     * Get list of students for filter dropdown.
     */
    @GetMapping("/messages/students")
    public ResponseEntity<?> getStudentsForFilter() {
        List<User> students = userRepository.findByRole(UserRole.STUDENT);
        return ResponseEntity.ok(students.stream()
            .map(s -> Map.of("id", s.getId(), "name", s.getFullName()))
            .toList()
        );
    }

    // ============= THUMBNAILS =============

    /**
     * Generate thumbnails for all lessons that have recordings but no thumbnails.
     */
    @PostMapping("/thumbnails/generate-missing")
    public ResponseEntity<?> generateMissingThumbnails() {
        if (!thumbnailService.isFfmpegAvailable()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "FFmpeg n'est pas disponible sur le serveur"
            ));
        }

        // Count lessons needing thumbnails
        long count = lessonRepository.findAll().stream()
            .filter(l -> l.getRecordingUrl() != null && l.getThumbnailUrl() == null)
            .count();

        if (count == 0) {
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Toutes les lecons ont deja des miniatures"
            ));
        }

        // Trigger async generation
        thumbnailService.generateMissingThumbnails();

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", count + " miniature(s) en cours de generation"
        ));
    }

    /**
     * Generate thumbnail for a specific lesson.
     */
    @PostMapping("/thumbnails/generate/{lessonId}")
    public ResponseEntity<?> generateThumbnail(@PathVariable Long lessonId) {
        if (!thumbnailService.isFfmpegAvailable()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "FFmpeg n'est pas disponible sur le serveur"
            ));
        }

        Lesson lesson = lessonRepository.findById(lessonId).orElse(null);
        if (lesson == null) {
            return ResponseEntity.notFound().build();
        }

        if (lesson.getRecordingUrl() == null) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Cette lecon n'a pas d'enregistrement video"
            ));
        }

        thumbnailService.generateThumbnailAsync(lessonId);

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Generation de la miniature en cours pour la lecon " + lessonId
        ));
    }

    // ============= BROADCAST EMAILS =============

    /**
     * Broadcast availability emails to all students for all coaches with active availabilities.
     * Used for platform launch to notify all users about available coaches.
     * TEMPORARY: Also exposed without auth at /api/launch/broadcast for one-time launch use.
     */
    @PostMapping("/broadcast-availabilities")
    public ResponseEntity<?> broadcastAvailabilities() {
        try {
            // Get all students
            List<User> students = userRepository.findByRole(UserRole.STUDENT);
            if (students.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Aucun etudiant dans la base",
                    "emailsSent", 0
                ));
            }

            // Get all teachers with active availabilities
            List<User> teachers = userRepository.findByRole(UserRole.TEACHER);
            List<Map<String, Object>> teachersWithAvailabilities = new ArrayList<>();

            for (User teacher : teachers) {
                List<Availability> availabilities = availabilityRepository.findByTeacherIdAndIsActiveTrue(teacher.getId());
                if (!availabilities.isEmpty()) {
                    // Build availability info string
                    StringBuilder availabilityInfo = new StringBuilder();
                    for (Availability a : availabilities) {
                        if (a.getIsRecurring()) {
                            String dayName = a.getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.FRENCH);
                            availabilityInfo.append("- Tous les ").append(dayName).append("s de ")
                                .append(a.getStartTime()).append(" a ").append(a.getEndTime()).append("\n");
                        } else if (a.getSpecificDate() != null && !a.getSpecificDate().isBefore(LocalDate.now())) {
                            availabilityInfo.append("- Le ").append(a.getSpecificDate()).append(" de ")
                                .append(a.getStartTime()).append(" a ").append(a.getEndTime()).append("\n");
                        }
                    }

                    if (availabilityInfo.length() > 0) {
                        teachersWithAvailabilities.add(Map.of(
                            "teacher", teacher,
                            "availabilityInfo", availabilityInfo.toString(),
                            "bookingLink", frontendUrl + "/book/" + teacher.getId()
                        ));
                    }
                }
            }

            if (teachersWithAvailabilities.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Aucun coach avec des disponibilites actives",
                    "emailsSent", 0
                ));
            }

            // Send emails to all students for each teacher with availabilities
            int emailsSent = 0;
            for (User student : students) {
                for (Map<String, Object> teacherData : teachersWithAvailabilities) {
                    User teacher = (User) teacherData.get("teacher");
                    String availabilityInfo = (String) teacherData.get("availabilityInfo");
                    String bookingLink = (String) teacherData.get("bookingLink");

                    emailService.sendNewAvailabilityNotification(
                        student.getEmail(),
                        student.getFirstName(),
                        teacher.getFirstName() + " " + teacher.getLastName(),
                        availabilityInfo,
                        bookingLink
                    );
                    emailsSent++;
                }
            }

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", emailsSent + " emails envoyes (" + students.size() + " etudiants x " + teachersWithAvailabilities.size() + " coachs)",
                "emailsSent", emailsSent,
                "studentsCount", students.size(),
                "teachersCount", teachersWithAvailabilities.size()
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Erreur: " + e.getMessage()
            ));
        }
    }
}
