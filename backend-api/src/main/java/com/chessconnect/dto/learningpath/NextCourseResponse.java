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
            case A -> "Pion";
            case B -> "Cavalier";
            case C -> "Reine";
            case D -> "Roi";
        };
        return new NextCourseResponse(courseId, title, grade, gradeName);
    }
}
