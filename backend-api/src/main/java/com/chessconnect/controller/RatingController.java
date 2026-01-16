package com.chessconnect.controller;

import com.chessconnect.dto.rating.CreateRatingRequest;
import com.chessconnect.dto.rating.RatingResponse;
import com.chessconnect.model.User;
import com.chessconnect.model.enums.UserRole;
import com.chessconnect.repository.UserRepository;
import com.chessconnect.service.RatingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ratings")
public class RatingController {

    private final RatingService ratingService;
    private final UserRepository userRepository;

    public RatingController(RatingService ratingService, UserRepository userRepository) {
        this.ratingService = ratingService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<RatingResponse> createRating(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateRatingRequest request
    ) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() != UserRole.STUDENT) {
            throw new RuntimeException("Only students can rate lessons");
        }

        RatingResponse rating = ratingService.createRating(user.getId(), request);
        return ResponseEntity.ok(rating);
    }

    @GetMapping("/teacher/{teacherId}")
    public ResponseEntity<List<RatingResponse>> getTeacherRatings(@PathVariable Long teacherId) {
        List<RatingResponse> ratings = ratingService.getRatingsForTeacher(teacherId);
        return ResponseEntity.ok(ratings);
    }

    @GetMapping("/lesson/{lessonId}")
    public ResponseEntity<Map<String, Object>> getLessonRating(@PathVariable Long lessonId) {
        boolean isRated = ratingService.isLessonRated(lessonId);
        RatingResponse rating = ratingService.getRatingForLesson(lessonId);
        return ResponseEntity.ok(Map.of(
                "isRated", isRated,
                "rating", rating != null ? rating : Map.of()
        ));
    }

    @GetMapping("/teacher/{teacherId}/summary")
    public ResponseEntity<Map<String, Object>> getTeacherRatingSummary(@PathVariable Long teacherId) {
        Double averageRating = ratingService.getAverageRatingForTeacher(teacherId);
        Integer reviewCount = ratingService.getReviewCountForTeacher(teacherId);
        return ResponseEntity.ok(Map.of(
                "averageRating", averageRating != null ? averageRating : 0.0,
                "reviewCount", reviewCount
        ));
    }
}
