package com.chessconnect.dto.learningpath;

import java.util.List;

public record LearningPathResponse(
    List<GradeWithCoursesResponse> grades,
    int totalCourses,
    int completedCourses,
    double overallProgressPercentage
) {
    public static LearningPathResponse create(List<GradeWithCoursesResponse> grades) {
        int totalCourses = grades.stream()
            .mapToInt(GradeWithCoursesResponse::totalCourses)
            .sum();
        int completedCourses = grades.stream()
            .mapToInt(GradeWithCoursesResponse::completedCourses)
            .sum();
        double overallProgressPercentage = totalCourses > 0
            ? (double) completedCourses / totalCourses * 100
            : 0;

        return new LearningPathResponse(
            grades,
            totalCourses,
            completedCourses,
            overallProgressPercentage
        );
    }
}
