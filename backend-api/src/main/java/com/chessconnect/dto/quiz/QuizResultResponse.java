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
        if (result.getPionScore() != null) scores.put(ChessLevel.PION, result.getPionScore());
        if (result.getCavalierScore() != null) scores.put(ChessLevel.CAVALIER, result.getCavalierScore());
        if (result.getFouScore() != null) scores.put(ChessLevel.FOU, result.getFouScore());
        if (result.getTourScore() != null) scores.put(ChessLevel.TOUR, result.getTourScore());
        if (result.getDameScore() != null) scores.put(ChessLevel.DAME, result.getDameScore());
        if (result.getRoiScore() != null) scores.put(ChessLevel.ROI, result.getRoiScore());

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
            case PION -> "Bienvenue dans le monde des échecs ! Vous commencez votre parcours au niveau Pion.";
            case CAVALIER -> "Vous avez de bonnes bases ! Vous démarrez au niveau Cavalier.";
            case FOU -> "Excellentes connaissances ! Vous êtes prêt pour le niveau Fou.";
            case TOUR -> "Vous êtes un joueur avancé ! Vous commencez au niveau Tour.";
            case DAME -> "Impressionnant ! Vous maîtrisez les échecs au niveau Dame.";
            case ROI -> "Exceptionnel ! Vous êtes un véritable maître des échecs au niveau Roi.";
        };
    }
}
