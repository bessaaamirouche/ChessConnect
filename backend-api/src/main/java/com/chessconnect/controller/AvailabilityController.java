package com.chessconnect.controller;

import com.chessconnect.dto.availability.AvailabilityRequest;
import com.chessconnect.dto.availability.AvailabilityResponse;
import com.chessconnect.dto.availability.TimeSlotResponse;
import com.chessconnect.security.UserDetailsImpl;
import com.chessconnect.service.AvailabilityService;
import com.chessconnect.service.SubscriptionService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/availabilities")
public class AvailabilityController {

    private static final Logger log = LoggerFactory.getLogger(AvailabilityController.class);

    private final AvailabilityService availabilityService;
    private final SubscriptionService subscriptionService;

    public AvailabilityController(AvailabilityService availabilityService, SubscriptionService subscriptionService) {
        this.availabilityService = availabilityService;
        this.subscriptionService = subscriptionService;
    }

    // Teacher: Create a new availability slot
    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<AvailabilityResponse> createAvailability(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody AvailabilityRequest request
    ) {
        AvailabilityResponse response = availabilityService.createAvailability(
                userDetails.getId(), request
        );
        return ResponseEntity.ok(response);
    }

    // Teacher: Get my availabilities
    @GetMapping("/me")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<AvailabilityResponse>> getMyAvailabilities(
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        List<AvailabilityResponse> availabilities = availabilityService.getTeacherAvailabilities(
                userDetails.getId()
        );
        return ResponseEntity.ok(availabilities);
    }

    // Teacher: Get my recurring availabilities
    @GetMapping("/me/recurring")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<AvailabilityResponse>> getMyRecurringAvailabilities(
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        List<AvailabilityResponse> availabilities = availabilityService.getRecurringAvailabilities(
                userDetails.getId()
        );
        return ResponseEntity.ok(availabilities);
    }

    // Teacher: Delete an availability
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Void> deleteAvailability(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long id
    ) {
        availabilityService.deleteAvailability(userDetails.getId(), id);
        return ResponseEntity.noContent().build();
    }

    // Public: Get a teacher's availabilities
    @GetMapping("/teacher/{teacherId}")
    public ResponseEntity<List<AvailabilityResponse>> getTeacherAvailabilities(
            @PathVariable Long teacherId
    ) {
        List<AvailabilityResponse> availabilities = availabilityService.getTeacherAvailabilities(teacherId);
        return ResponseEntity.ok(availabilities);
    }

    // Get available time slots for a teacher
    // Premium users see slots 24h before non-premium users
    @GetMapping("/teacher/{teacherId}/slots")
    public ResponseEntity<List<TimeSlotResponse>> getAvailableSlots(
            @PathVariable Long teacherId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        // Limit to 30 days max
        if (endDate.isAfter(startDate.plusDays(30))) {
            endDate = startDate.plusDays(30);
        }

        // Check if user is premium for priority access
        boolean isPremium = userDetails != null && subscriptionService.isPremium(userDetails.getId());

        log.info("Getting slots for teacher {}: user={}, isPremium={}, startDate={}, endDate={}",
                teacherId,
                userDetails != null ? userDetails.getId() : "anonymous",
                isPremium,
                startDate,
                endDate);

        List<TimeSlotResponse> slots = availabilityService.getAvailableSlots(teacherId, startDate, endDate, isPremium);
        log.info("Returning {} slots for teacher {}", slots.size(), teacherId);
        return ResponseEntity.ok(slots);
    }
}
