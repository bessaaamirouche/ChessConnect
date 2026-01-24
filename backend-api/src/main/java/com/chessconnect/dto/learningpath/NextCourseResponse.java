package com.chessconnect.dto.learningpath;

import com.chessconnect.model.enums.ChessLevel;

public record NextCourseResponse(
    Long courseId,
    String title,
    ChessLevel grade,
    String gradeName
) {
    public static NextCourseResponse create(Long courseId, String title, ChessLevel grade) {
        String gradeName = switch (grade) {
            case PION -> "Pion";
            case CAVALIER -> "Cavalier";
            case FOU -> "Fou";
            case TOUR -> "Tour";
            case DAME -> "Dame";
        };
        return new NextCourseResponse(courseId, title, grade, gradeName);
    }
}
