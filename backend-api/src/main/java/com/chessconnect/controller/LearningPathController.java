package com.chessconnect.controller;

import com.chessconnect.dto.learningpath.CourseResponse;
import com.chessconnect.dto.learningpath.LearningPathResponse;
import com.chessconnect.dto.learningpath.NextCourseResponse;
import com.chessconnect.dto.student.StudentProfileResponse;
import com.chessconnect.security.UserDetailsImpl;
import com.chessconnect.service.LearningPathService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/learning-path")
public class LearningPathController {

    private final LearningPathService learningPathService;

    public LearningPathController(LearningPathService learningPathService) {
        this.learningPathService = learningPathService;
    }

    @GetMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<LearningPathResponse> getLearningPath(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        LearningPathResponse response = learningPathService.getLearningPath(userDetails.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/courses/{id}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<CourseResponse> getCourseDetail(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long id) {
        CourseResponse response = learningPathService.getCourseDetail(userDetails.getId(), id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/courses/{id}/start")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<CourseResponse> startCourse(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long id) {
        CourseResponse response = learningPathService.startCourse(userDetails.getId(), id);
        return ResponseEntity.ok(response);
    }

    /**
     * Validate a course for a student (Teacher only)
     */
    @PostMapping("/courses/{courseId}/validate/{studentId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<CourseResponse> validateCourse(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long courseId,
            @PathVariable Long studentId) {
        CourseResponse response = learningPathService.validateCourse(
            userDetails.getId(),
            studentId,
            courseId
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Get student profile with course progress (Teacher only)
     */
    @GetMapping("/students/{studentId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<StudentProfileResponse> getStudentProfile(
            @PathVariable Long studentId) {
        StudentProfileResponse response = learningPathService.getStudentProfile(studentId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get next course for a student (Teacher only)
     */
    @GetMapping("/students/{studentId}/next-course")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<NextCourseResponse> getNextCourse(
            @PathVariable Long studentId) {
        NextCourseResponse response = learningPathService.getNextCourseForStudent(studentId);
        if (response == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(response);
    }
}
