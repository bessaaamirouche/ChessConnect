package com.chessconnect.controller;

import com.chessconnect.dto.exercise.ExerciseResponse;
import com.chessconnect.security.UserDetailsImpl;
import com.chessconnect.service.ExerciseService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/exercises")
public class ExerciseController {

    private final ExerciseService exerciseService;

    public ExerciseController(ExerciseService exerciseService) {
        this.exerciseService = exerciseService;
    }

    @GetMapping("/lesson/{lessonId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ExerciseResponse> getExerciseForLesson(
        @PathVariable Long lessonId,
        @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        ExerciseResponse response = exerciseService.getExerciseForLesson(
            lessonId, userDetails.getId()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{exerciseId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ExerciseResponse> getExerciseById(
        @PathVariable Long exerciseId,
        @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        ExerciseResponse response = exerciseService.getExerciseById(
            exerciseId, userDetails.getId()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<ExerciseResponse>> getAllExercises(
        @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        List<ExerciseResponse> exercises = exerciseService.getAllExercisesForUser(
            userDetails.getId()
        );
        return ResponseEntity.ok(exercises);
    }
}
