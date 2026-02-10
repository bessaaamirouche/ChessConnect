package com.chessconnect.dto.group;

import com.chessconnect.dto.lesson.LessonResponse;
import com.chessconnect.model.Lesson;

import java.time.LocalDateTime;
import java.util.List;

public record GroupLessonResponse(
    LessonResponse lesson,
    boolean isGroupLesson,
    Integer maxParticipants,
    String groupStatus,
    Integer currentParticipantCount,
    Integer pricePerPersonCents,
    String invitationToken,
    LocalDateTime deadline,
    List<ParticipantSummary> participants
) {
    public static GroupLessonResponse from(Lesson lesson, String invitationToken, int pricePerPersonCents, LocalDateTime deadline) {
        List<ParticipantSummary> participants = lesson.getActiveParticipants().stream()
                .map(ParticipantSummary::from)
                .toList();

        return new GroupLessonResponse(
                LessonResponse.from(lesson),
                true,
                lesson.getMaxParticipants(),
                lesson.getGroupStatus(),
                lesson.getActiveParticipantCount(),
                pricePerPersonCents,
                invitationToken,
                deadline,
                participants
        );
    }
}
