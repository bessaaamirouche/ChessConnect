package com.chessconnect.dto.favorite;

import com.chessconnect.model.FavoriteTeacher;

import java.time.LocalDateTime;
import java.util.List;

public record FavoriteTeacherResponse(
    Long id,
    Long teacherId,
    String teacherFirstName,
    String teacherLastName,
    String teacherEmail,
    Integer teacherHourlyRateCents,
    String teacherBio,
    String teacherAvatarUrl,
    List<String> teacherLanguages,
    Boolean notifyNewSlots,
    LocalDateTime createdAt
) {
    public static FavoriteTeacherResponse from(FavoriteTeacher favorite) {
        var teacher = favorite.getTeacher();
        List<String> languages = teacher.getLanguages() != null && !teacher.getLanguages().isEmpty()
                ? List.of(teacher.getLanguages().split(","))
                : List.of("FR");

        return new FavoriteTeacherResponse(
            favorite.getId(),
            teacher.getId(),
            teacher.getFirstName(),
            teacher.getLastName(),
            teacher.getEmail(),
            teacher.getHourlyRateCents(),
            teacher.getBio(),
            teacher.getAvatarUrl(),
            languages,
            favorite.getNotifyNewSlots(),
            favorite.getCreatedAt()
        );
    }
}
