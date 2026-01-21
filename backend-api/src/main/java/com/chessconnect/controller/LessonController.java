package com.chessconnect.controller;

import com.chessconnect.dto.lesson.BookLessonRequest;
import com.chessconnect.dto.lesson.LessonResponse;
import com.chessconnect.dto.lesson.UpdateLessonStatusRequest;
import com.chessconnect.security.UserDetailsImpl;
import com.chessconnect.service.LessonService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/lessons")
public class LessonController {

    private final LessonService lessonService;

    public LessonController(LessonService lessonService) {
        this.lessonService = lessonService;
    }

    @PostMapping("/book")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<LessonResponse> bookLesson(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody BookLessonRequest request
    ) {
        LessonResponse response = lessonService.bookLesson(userDetails.getId(), request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{lessonId}/status")
    public ResponseEntity<LessonResponse> updateLessonStatus(
            @PathVariable Long lessonId,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody UpdateLessonStatusRequest request
    ) {
        LessonResponse response = lessonService.updateLessonStatus(lessonId, userDetails.getId(), request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{lessonId}")
    public ResponseEntity<LessonResponse> getLesson(
            @PathVariable Long lessonId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        LessonResponse response = lessonService.getLessonById(lessonId, userDetails.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<LessonResponse>> getUpcomingLessons(
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        boolean isTeacher = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_TEACHER"));

        List<LessonResponse> lessons = isTeacher
                ? lessonService.getUpcomingLessonsForTeacher(userDetails.getId())
                : lessonService.getUpcomingLessonsForStudent(userDetails.getId());

        return ResponseEntity.ok(lessons);
    }

    @GetMapping("/history")
    public ResponseEntity<List<LessonResponse>> getLessonHistory(
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        List<LessonResponse> lessons = lessonService.getLessonHistory(userDetails.getId());
        return ResponseEntity.ok(lessons);
    }

    @DeleteMapping("/{lessonId}")
    public ResponseEntity<Void> deleteLesson(
            @PathVariable Long lessonId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        lessonService.deleteLesson(lessonId, userDetails.getId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Check if current student is eligible for a free trial lesson.
     */
    @GetMapping("/free-trial/eligible")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Map<String, Boolean>> checkFreeTrialEligibility(
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        boolean eligible = lessonService.isEligibleForFreeTrial(userDetails.getId());
        return ResponseEntity.ok(Map.of("eligible", eligible));
    }

    /**
     * Book a free trial lesson (first lesson is free for new students).
     */
    @PostMapping("/free-trial/book")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> bookFreeTrialLesson(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody BookLessonRequest request
    ) {
        try {
            LessonResponse response = lessonService.bookFreeTrialLesson(userDetails.getId(), request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Mark that the teacher has joined the video call.
     * This allows the student to see the "Accéder" button.
     */
    @PatchMapping("/{lessonId}/teacher-joined")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<LessonResponse> markTeacherJoined(
            @PathVariable Long lessonId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        LessonResponse response = lessonService.markTeacherJoined(lessonId, userDetails.getId());
        return ResponseEntity.ok(response);
    }

    /**
     * Check if teacher has joined the video call.
     */
    @GetMapping("/{lessonId}/teacher-joined")
    public ResponseEntity<Map<String, Boolean>> hasTeacherJoined(
            @PathVariable Long lessonId
    ) {
        boolean joined = lessonService.hasTeacherJoined(lessonId);
        return ResponseEntity.ok(Map.of("teacherJoined", joined));
    }
}
