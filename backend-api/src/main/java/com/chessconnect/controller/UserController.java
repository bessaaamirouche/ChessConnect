package com.chessconnect.controller;

import com.chessconnect.dto.user.ChangePasswordRequest;
import com.chessconnect.dto.user.TeacherProfileResponse;
import com.chessconnect.dto.user.UpdateProfileRequest;
import com.chessconnect.dto.user.UpdateTeacherProfileRequest;
import com.chessconnect.model.User;
import com.chessconnect.model.enums.UserRole;
import com.chessconnect.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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
            if (request.getBio() != null) {
                user.setBio(request.getBio());
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
        if (request.getAcceptsSubscription() != null) {
            user.setAcceptsSubscription(request.getAcceptsSubscription());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }

        user = userRepository.save(user);

        return ResponseEntity.ok(mapToProfileResponse(user));
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
                .bio(user.getBio())
                .avatarUrl(user.getAvatarUrl())
                .birthDate(user.getBirthDate())
                .eloRating(user.getEloRating())
                .emailRemindersEnabled(user.getEmailRemindersEnabled())
                .build();
    }
}
