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
}
