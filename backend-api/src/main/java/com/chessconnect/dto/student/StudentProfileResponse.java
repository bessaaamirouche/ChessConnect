package com.chessconnect.dto.student;

import com.chessconnect.dto.learningpath.GradeWithCoursesResponse;
import com.chessconnect.model.enums.ChessLevel;

import java.util.List;

public record StudentProfileResponse(
    Long id,
    String firstName,
    String lastName,
    String fullName,
    ChessLevel currentLevel,
    String currentLevelDisplayName,
    Integer totalLessonsCompleted,
    Double progressPercentage,
    List<GradeWithCoursesResponse> courseProgress
) {
    public static StudentProfileResponse create(
        Long id,
        String firstName,
        String lastName,
        ChessLevel currentLevel,
        Integer totalLessonsCompleted,
        List<GradeWithCoursesResponse> courseProgress
    ) {
        int totalCourses = courseProgress.stream()
            .mapToInt(GradeWithCoursesResponse::totalCourses)
            .sum();
        int completedCourses = courseProgress.stream()
            .mapToInt(GradeWithCoursesResponse::completedCourses)
            .sum();
        double progressPercentage = totalCourses > 0
            ? (double) completedCourses / totalCourses * 100
            : 0;

        return new StudentProfileResponse(
            id,
            firstName,
            lastName,
            firstName + " " + lastName,
            currentLevel,
            currentLevel.getDisplayName(),
            totalLessonsCompleted,
            progressPercentage,
            courseProgress
        );
    }
}
