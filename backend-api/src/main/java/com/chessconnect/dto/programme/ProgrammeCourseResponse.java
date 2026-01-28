package com.chessconnect.dto.programme;

public record ProgrammeCourseResponse(
    Integer id,
    String levelCode,
    String levelName,
    Integer courseOrder,
    String title,
    boolean isCurrent,
    boolean isCompleted
) {}
