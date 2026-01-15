package com.chessconnect.dto.learningpath;

import com.chessconnect.model.enums.ChessLevel;

import java.util.List;

public record GradeWithCoursesResponse(
    ChessLevel grade,
    String displayName,
    String description,
    int order,
    List<CourseResponse> courses,
    int totalCourses,
    int completedCourses,
    double progressPercentage,
    boolean isUnlocked,
    boolean isCompleted
) {
    public static GradeWithCoursesResponse create(
        ChessLevel grade,
        List<CourseResponse> courses,
        int completedCourses,
        boolean isUnlocked
    ) {
        int totalCourses = courses.size();
        double progressPercentage = totalCourses > 0
            ? (double) completedCourses / totalCourses * 100
            : 0;
        boolean isCompleted = completedCourses == totalCourses && totalCourses > 0;

        return new GradeWithCoursesResponse(
            grade,
            grade.getDisplayName(),
            grade.getDescription(),
            grade.getOrder(),
            courses,
            totalCourses,
            completedCourses,
            progressPercentage,
            isUnlocked,
            isCompleted
        );
    }
}
