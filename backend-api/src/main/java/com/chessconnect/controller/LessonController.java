package com.chessconnect.controller;

import com.chessconnect.dto.lesson.BookLessonRequest;
import com.chessconnect.dto.lesson.LessonResponse;
import com.chessconnect.dto.lesson.UpdateLessonStatusRequest;
import com.chessconnect.dto.PendingValidationResponse;
import com.chessconnect.security.UserDetailsImpl;
import com.chessconnect.service.LessonService;
import com.chessconnect.service.PendingValidationService;
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
    private final PendingValidationService pendingValidationService;

    public LessonController(LessonService lessonService, PendingValidationService pendingValidationService) {
        this.lessonService = lessonService;
        this.pendingValidationService = pendingValidationService;
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
     * Add or update a teacher comment on a completed lesson.
     * The comment is visible to the student and can be updated at any time
     * as long as the lesson is still in the teacher's history.
     */
    @PatchMapping("/{lessonId}/comment")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<LessonResponse> updateTeacherComment(
            @PathVariable Long lessonId,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody Map<String, String> request
    ) {
        String comment = request.get("comment");
        LessonResponse response = lessonService.updateTeacherComment(lessonId, userDetails.getId(), comment);
        return ResponseEntity.ok(response);
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

    /**
     * Get pending course validations for the current teacher.
     */
    @GetMapping("/pending-validations")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<PendingValidationResponse>> getPendingValidations(
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        List<PendingValidationResponse> validations = pendingValidationService.getPendingValidations(userDetails.getId());
        return ResponseEntity.ok(validations);
    }

    /**
     * Dismiss a pending validation.
     */
    @DeleteMapping("/pending-validations/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Void> dismissPendingValidation(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        pendingValidationService.dismissPendingValidation(id, userDetails.getId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Get count of pending validations for the current teacher.
     */
    @GetMapping("/pending-validations/count")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Map<String, Integer>> getPendingValidationsCount(
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        int count = pendingValidationService.getPendingValidationCount(userDetails.getId());
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * Check if this is the first completed lesson between the current teacher and the specified student.
     * Used to determine whether to show the level evaluation modal or the course validation modal.
     */
    @GetMapping("/is-first-lesson/{studentId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Map<String, Object>> isFirstLesson(
            @PathVariable Long studentId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        Map<String, Object> result = lessonService.checkFirstLesson(userDetails.getId(), studentId);
        return ResponseEntity.ok(result);
    }
}
