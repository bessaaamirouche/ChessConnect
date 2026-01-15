package com.chessconnect.dto.quiz;

import com.chessconnect.model.QuizQuestion;
import com.chessconnect.model.enums.ChessLevel;

public record QuizQuestionResponse(
        Long id,
        ChessLevel level,
        String question,
        String optionA,
        String optionB,
        String optionC,
        String optionD,
        Integer orderInLevel
) {
    public static QuizQuestionResponse from(QuizQuestion question) {
        return new QuizQuestionResponse(
                question.getId(),
                question.getLevel(),
                question.getQuestion(),
                question.getOptionA(),
                question.getOptionB(),
                question.getOptionC(),
                question.getOptionD(),
                question.getOrderInLevel()
        );
    }
}
