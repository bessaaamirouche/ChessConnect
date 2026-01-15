package com.chessconnect.controller;

import com.chessconnect.dto.quiz.QuizQuestionResponse;
import com.chessconnect.dto.quiz.QuizResultResponse;
import com.chessconnect.dto.quiz.QuizSubmitRequest;
import com.chessconnect.security.UserDetailsImpl;
import com.chessconnect.service.QuizService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/quiz")
public class QuizController {

    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    /**
     * Get all quiz questions.
     * Accessible to authenticated users (students).
     */
    @GetMapping("/questions")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<QuizQuestionResponse>> getQuestions() {
        return ResponseEntity.ok(quizService.getQuizQuestions());
    }

    /**
     * Submit quiz answers and get the result.
     * Updates the student's level based on quiz performance.
     */
    @PostMapping("/submit")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<QuizResultResponse> submitQuiz(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody QuizSubmitRequest request
    ) {
        QuizResultResponse result = quizService.evaluateQuiz(userDetails.getId(), request);
        return ResponseEntity.ok(result);
    }

    /**
     * Get the last quiz result for the current student.
     */
    @GetMapping("/result")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<QuizResultResponse> getLastResult(
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        return quizService.getLastResult(userDetails.getId())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
