package com.chessconnect.dto.quiz;

import java.util.List;

public record QuizSubmitRequest(
        List<QuizAnswerRequest> answers
) {}
