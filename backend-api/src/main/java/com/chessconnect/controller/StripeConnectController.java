package com.chessconnect.controller;

import com.chessconnect.model.User;
import com.chessconnect.model.enums.UserRole;
import com.chessconnect.repository.UserRepository;
import com.chessconnect.security.UserDetailsImpl;
import com.chessconnect.service.StripeConnectService;
import com.chessconnect.service.StripeConnectService.AccountStatus;
import com.chessconnect.service.StripeConnectService.OnboardingResult;
import com.stripe.exception.StripeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/stripe-connect")
public class StripeConnectController {

    private static final Logger log = LoggerFactory.getLogger(StripeConnectController.class);

    private final StripeConnectService stripeConnectService;
    private final UserRepository userRepository;

    public StripeConnectController(
            StripeConnectService stripeConnectService,
            UserRepository userRepository
    ) {
        this.stripeConnectService = stripeConnectService;
        this.userRepository = userRepository;
    }

    /**
     * Get Stripe Connect onboarding URL for a teacher.
     * Creates a new Express account if needed and returns the Stripe-hosted onboarding URL.
     */
    @PostMapping("/onboarding")
    @PreAuthorize("hasRole('TEACHER')")
    @Transactional
    public ResponseEntity<Map<String, Object>> createOnboardingUrl(
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        try {
            User teacher = userRepository.findById(userDetails.getId())
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouve"));

            if (teacher.getRole() != UserRole.TEACHER) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Seuls les coachs peuvent configurer Stripe Connect"
                ));
            }

            // Create account (if needed) and get onboarding URL
            OnboardingResult result = stripeConnectService.createOnboardingUrl(teacher);

            // Save the account ID if a new account was created
            if (result.newAccount()) {
                teacher.setStripeConnectAccountId(result.accountId());
                teacher.setStripeConnectOnboardingComplete(false);
                userRepository.save(teacher);
                log.info("Created new Stripe Connect account {} for teacher {}", result.accountId(), teacher.getId());
            }

            log.info("Generated Stripe Connect onboarding URL for teacher {}", teacher.getId());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "onboardingUrl", result.url(),
                    "accountId", result.accountId()
            ));

        } catch (StripeException e) {
            log.error("Stripe error during onboarding", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                    "success", false,
                    "message", "Erreur Stripe: " + e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error during Stripe Connect onboarding", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Get Stripe Connect account status for a teacher.
     */
    @GetMapping("/status")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Map<String, Object>> getAccountStatus(
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        try {
            User teacher = userRepository.findById(userDetails.getId())
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouve"));

            String accountId = teacher.getStripeConnectAccountId();

            if (accountId == null || accountId.isBlank()) {
                return ResponseEntity.ok(Map.of(
                        "connected", false,
                        "accountExists", false,
                        "isReady", false,
                        "message", "Aucun compte Stripe Connect configure"
                ));
            }

            AccountStatus status = stripeConnectService.getAccountStatus(accountId);

            // Update onboarding status in database if needed
            if (status.isReady() && !Boolean.TRUE.equals(teacher.getStripeConnectOnboardingComplete())) {
                teacher.setStripeConnectOnboardingComplete(true);
                userRepository.save(teacher);
            }

            return ResponseEntity.ok(Map.of(
                    "connected", true,
                    "accountExists", status.accountExists(),
                    "isReady", status.isReady(),
                    "chargesEnabled", status.chargesEnabled(),
                    "payoutsEnabled", status.payoutsEnabled(),
                    "pendingReason", status.pendingReason() != null ? status.pendingReason() : "",
                    "message", status.isReady() ? "Compte pret a recevoir des paiements" : "Configuration en cours"
            ));

        } catch (StripeException e) {
            log.error("Stripe error getting account status", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                    "connected", false,
                    "accountExists", false,
                    "isReady", false,
                    "message", "Erreur de communication avec Stripe"
            ));
        } catch (Exception e) {
            log.error("Error getting Stripe Connect status", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "connected", false,
                    "accountExists", false,
                    "isReady", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Refresh the onboarding link (called after user returns from incomplete onboarding).
     */
    @PostMapping("/refresh-link")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Map<String, Object>> refreshOnboardingLink(
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        try {
            User teacher = userRepository.findById(userDetails.getId())
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouve"));

            String accountId = teacher.getStripeConnectAccountId();
            if (accountId == null || accountId.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Aucun compte Stripe Connect a rafraichir"
                ));
            }

            OnboardingResult result = stripeConnectService.createOnboardingUrl(teacher);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "onboardingUrl", result.url()
            ));

        } catch (StripeException e) {
            log.error("Stripe error refreshing onboarding link", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                    "success", false,
                    "message", "Erreur Stripe: " + e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error refreshing Stripe Connect onboarding link", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Disconnect Stripe Connect account.
     * This removes the account ID from the user but doesn't delete the Stripe account.
     */
    @DeleteMapping("/disconnect")
    @PreAuthorize("hasRole('TEACHER')")
    @Transactional
    public ResponseEntity<Map<String, Object>> disconnectAccount(
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        try {
            User teacher = userRepository.findById(userDetails.getId())
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouve"));

            teacher.setStripeConnectAccountId(null);
            teacher.setStripeConnectOnboardingComplete(false);
            userRepository.save(teacher);

            log.info("Disconnected Stripe Connect for teacher {}", teacher.getId());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Compte Stripe Connect deconnecte"
            ));

        } catch (Exception e) {
            log.error("Error disconnecting Stripe Connect", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
}
