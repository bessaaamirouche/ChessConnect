package com.chessconnect.dto.quiz;

import com.chessconnect.model.QuizResult;
import com.chessconnect.model.enums.ChessLevel;

import java.util.HashMap;
import java.util.Map;

public record QuizResultResponse(
        ChessLevel determinedLevel,
        String levelDisplayName,
        String levelDescription,
        Map<ChessLevel, Integer> scoresByLevel,
        Map<ChessLevel, Integer> totalByLevel,
        String message
) {
    public static QuizResultResponse from(QuizResult result, Map<ChessLevel, Integer> totals) {
        Map<ChessLevel, Integer> scores = new HashMap<>();
        if (result.getLevelAScore() != null) scores.put(ChessLevel.A, result.getLevelAScore());
        if (result.getLevelBScore() != null) scores.put(ChessLevel.B, result.getLevelBScore());
        if (result.getLevelCScore() != null) scores.put(ChessLevel.C, result.getLevelCScore());
        if (result.getLevelDScore() != null) scores.put(ChessLevel.D, result.getLevelDScore());

        ChessLevel level = result.getDeterminedLevel();
        String message = generateMessage(level);

        return new QuizResultResponse(
                level,
                level.getDisplayName(),
                level.getDescription(),
                scores,
                totals,
                message
        );
    }

    private static String generateMessage(ChessLevel level) {
        return switch (level) {
            case A -> "Bienvenue dans le monde des échecs ! Vous commencez votre parcours au niveau Pion.";
            case B -> "Vous avez de bonnes bases ! Vous démarrez au niveau Cavalier.";
            case C -> "Excellentes connaissances ! Vous êtes prêt pour le niveau Reine.";
            case D -> "Impressionnant ! Vous maîtrisez les échecs au niveau Roi.";
        };
    }
}
