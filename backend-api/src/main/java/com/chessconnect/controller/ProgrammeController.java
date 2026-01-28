package com.chessconnect.controller;

import com.chessconnect.dto.programme.ProgrammeCourseResponse;
import com.chessconnect.dto.programme.UpdateCurrentCourseRequest;
import com.chessconnect.security.UserDetailsImpl;
import com.chessconnect.service.ProgrammeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/programme")
public class ProgrammeController {

    private final ProgrammeService programmeService;

    public ProgrammeController(ProgrammeService programmeService) {
        this.programmeService = programmeService;
    }

    /**
     * Get all courses in the programme with current/completed status for user
     */
    @GetMapping("/courses")
    public ResponseEntity<List<ProgrammeCourseResponse>> getAllCourses(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Long userId = userDetails.getId();
        return ResponseEntity.ok(programmeService.getAllCourses(userId));
    }

    /**
     * Get courses grouped by level
     */
    @GetMapping("/courses/by-level")
    public ResponseEntity<Map<String, List<ProgrammeCourseResponse>>> getCoursesByLevel(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Long userId = userDetails.getId();
        return ResponseEntity.ok(programmeService.getCoursesByLevel(userId));
    }

    /**
     * Get the current course for the user
     */
    @GetMapping("/current")
    public ResponseEntity<ProgrammeCourseResponse> getCurrentCourse(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Long userId = userDetails.getId();
        return ResponseEntity.ok(programmeService.getCurrentCourse(userId));
    }

    /**
     * Set the starting course (used at registration or by student to go back)
     */
    @PostMapping("/current")
    public ResponseEntity<ProgrammeCourseResponse> setCurrentCourse(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody UpdateCurrentCourseRequest body) {
        Long userId = userDetails.getId();
        return ResponseEntity.ok(programmeService.setCurrentCourse(userId, body.courseId()));
    }

    /**
     * Advance to the next course (called after lesson completion)
     */
    @PostMapping("/advance")
    public ResponseEntity<ProgrammeCourseResponse> advanceToNextCourse(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Long userId = userDetails.getId();
        return ResponseEntity.ok(programmeService.advanceToNextCourse(userId));
    }

    /**
     * Go back to the previous course
     */
    @PostMapping("/go-back")
    public ResponseEntity<ProgrammeCourseResponse> goBackToPreviousCourse(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Long userId = userDetails.getId();
        return ResponseEntity.ok(programmeService.goBackToPreviousCourse(userId));
    }

    /**
     * Public endpoint - Get all courses (for registration page)
     */
    @GetMapping("/public/courses")
    public ResponseEntity<List<ProgrammeCourseResponse>> getPublicCourses() {
        // Return all courses with no user-specific status
        return ResponseEntity.ok(programmeService.getAllCourses(null));
    }
}
