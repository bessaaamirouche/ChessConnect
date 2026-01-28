package com.chessconnect.controller;

import com.chessconnect.dto.user.ChangePasswordRequest;
import com.chessconnect.dto.user.DeleteAccountRequest;
import com.chessconnect.dto.user.TeacherProfileResponse;
import com.chessconnect.dto.user.UpdateProfileRequest;
import com.chessconnect.dto.user.UpdateTeacherProfileRequest;
import com.chessconnect.model.User;
import com.chessconnect.model.enums.UserRole;
import com.chessconnect.repository.UserRepository;
import com.chessconnect.service.AdminService;
import com.chessconnect.service.WalletService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminService adminService;
    private final WalletService walletService;

    public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder, AdminService adminService, WalletService walletService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminService = adminService;
        this.walletService = walletService;
    }

    @GetMapping("/me")
    public ResponseEntity<TeacherProfileResponse> getCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(mapToProfileResponse(user));
    }

    @PatchMapping("/me")
    public ResponseEntity<TeacherProfileResponse> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody UpdateProfileRequest request) {

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getFirstName() != null && !request.getFirstName().isBlank()) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null && !request.getLastName().isBlank()) {
            user.setLastName(request.getLastName());
        }

        // Teacher-specific fields
        if (user.getRole() == UserRole.TEACHER) {
            if (request.getHourlyRateCents() != null) {
                user.setHourlyRateCents(request.getHourlyRateCents());
            }
            if (request.getAcceptsSubscription() != null) {
                user.setAcceptsSubscription(request.getAcceptsSubscription());
            }
            if (request.getAcceptsFreeTrial() != null) {
                user.setAcceptsFreeTrial(request.getAcceptsFreeTrial());
            }
            if (request.getBio() != null) {
                user.setBio(request.getBio());
            }
            if (request.getLanguages() != null && !request.getLanguages().isEmpty()) {
                user.setLanguages(String.join(",", request.getLanguages()));
            }
            // Banking fields
            if (request.getIban() != null) {
                user.setIban(request.getIban());
            }
            if (request.getBic() != null) {
                user.setBic(request.getBic());
            }
            if (request.getAccountHolderName() != null) {
                user.setAccountHolderName(request.getAccountHolderName());
            }
            if (request.getSiret() != null) {
                user.setSiret(request.getSiret());
            }
            if (request.getCompanyName() != null) {
                user.setCompanyName(request.getCompanyName());
            }
        }

        // Student-specific fields
        if (user.getRole() == UserRole.STUDENT) {
            if (request.getBirthDate() != null) {
                user.setBirthDate(request.getBirthDate());
            }
            if (request.getEloRating() != null) {
                user.setEloRating(request.getEloRating());
            }
        }

        // Preferences (for all users)
        if (request.getEmailRemindersEnabled() != null) {
            user.setEmailRemindersEnabled(request.getEmailRemindersEnabled());
        }

        user = userRepository.save(user);

        return ResponseEntity.ok(mapToProfileResponse(user));
    }

    @PostMapping("/me/password")
    public ResponseEntity<?> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Mot de passe actuel incorrect"));
        }

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Mot de passe modifié avec succès"));
    }

    @PutMapping("/me/teacher-profile")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<TeacherProfileResponse> updateTeacherProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateTeacherProfileRequest request) {

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getHourlyRateCents() != null) {
            user.setHourlyRateCents(request.getHourlyRateCents());
        }
        if (request.getAcceptsFreeTrial() != null) {
            user.setAcceptsFreeTrial(request.getAcceptsFreeTrial());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }

        user = userRepository.save(user);

        return ResponseEntity.ok(mapToProfileResponse(user));
    }

    /**
     * Heartbeat endpoint to update user's online presence.
     * Called every 30 seconds by the frontend.
     */
    @PostMapping("/me/heartbeat")
    public ResponseEntity<Void> heartbeat(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setLastActiveAt(LocalDateTime.now());
        userRepository.save(user);

        return ResponseEntity.ok().build();
    }

    /**
     * Delete current user's account (RGPD compliance).
     * Requires password verification for security.
     * Deletes all user data including lessons, ratings, etc.
     */
    @DeleteMapping("/me")
    public ResponseEntity<?> deleteAccount(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody DeleteAccountRequest request) {

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Prevent admin self-deletion (admins must be deleted by other admins)
        if (user.getRole() == UserRole.ADMIN) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Les administrateurs ne peuvent pas supprimer leur propre compte"));
        }

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Mot de passe incorrect"));
        }

        // Check if student has wallet balance - prevent deletion if so
        if (user.getRole() == UserRole.STUDENT) {
            int walletBalance = walletService.getBalance(user.getId());
            if (walletBalance > 0) {
                String balanceFormatted = String.format("%.2f", walletBalance / 100.0);
                return ResponseEntity.badRequest()
                        .body(Map.of(
                            "message", "Vous avez " + balanceFormatted + " EUR dans votre portefeuille. " +
                                      "Veuillez utiliser vos credits ou contacter le support pour un remboursement avant de supprimer votre compte.",
                            "walletBalance", walletBalance
                        ));
            }
        }

        log.info("User {} ({}) requested account deletion", user.getId(), user.getEmail());

        try {
            // Reuse admin deletion logic which handles all related data
            adminService.deleteUser(user.getId());
            log.info("User account {} deleted successfully (self-deletion)", user.getId());
            return ResponseEntity.ok(Map.of("message", "Votre compte a ete supprime avec succes"));
        } catch (Exception e) {
            log.error("Failed to delete user account {}: {}", user.getId(), e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Erreur lors de la suppression du compte"));
        }
    }

    private TeacherProfileResponse mapToProfileResponse(User user) {
        return TeacherProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .hourlyRateCents(user.getHourlyRateCents())
                .acceptsSubscription(user.getAcceptsSubscription())
                .acceptsFreeTrial(user.getAcceptsFreeTrial())
                .bio(user.getBio())
                .avatarUrl(user.getAvatarUrl())
                .languages(TeacherProfileResponse.parseLanguages(user.getLanguages()))
                .iban(user.getIban())
                .bic(user.getBic())
                .accountHolderName(user.getAccountHolderName())
                .siret(user.getSiret())
                .companyName(user.getCompanyName())
                .birthDate(user.getBirthDate())
                .eloRating(user.getEloRating())
                .emailRemindersEnabled(user.getEmailRemindersEnabled())
                .build();
    }
}
