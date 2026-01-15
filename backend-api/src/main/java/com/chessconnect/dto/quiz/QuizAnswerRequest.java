package com.chessconnect.dto.quiz;

public record QuizAnswerRequest(
        Long questionId,
        String answer
) {}
