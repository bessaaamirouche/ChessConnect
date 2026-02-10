package com.chessconnect.dto.group;

import com.chessconnect.model.GroupInvitation;
import com.chessconnect.model.Lesson;
import com.chessconnect.model.User;

import java.time.LocalDateTime;
import java.util.List;

public record GroupInvitationResponse(
    String token,
    Long lessonId,
    String teacherFirstName,
    String teacherLastInitial,
    String teacherAvatarUrl,
    LocalDateTime scheduledAt,
    Integer durationMinutes,
    Integer targetGroupSize,
    Integer currentParticipantCount,
    Integer spotsRemaining,
    Integer pricePerPersonCents,
    LocalDateTime deadline,
    boolean isExpired,
    boolean isFull,
    List<ParticipantSummary> participants
) {
    public static GroupInvitationResponse from(GroupInvitation invitation, int pricePerPersonCents) {
        Lesson lesson = invitation.getLesson();
        User teacher = lesson.getTeacher();
        int activeCount = lesson.getActiveParticipantCount();
        int maxP = invitation.getMaxParticipants();

        String lastInitial = teacher.getLastName() != null && !teacher.getLastName().isEmpty()
                ? String.valueOf(teacher.getLastName().charAt(0)) + "."
                : "";

        List<ParticipantSummary> participants = lesson.getActiveParticipants().stream()
                .map(ParticipantSummary::from)
                .toList();

        return new GroupInvitationResponse(
                invitation.getToken(),
                lesson.getId(),
                teacher.getFirstName(),
                lastInitial,
                teacher.getAvatarUrl(),
                lesson.getScheduledAt(),
                lesson.getDurationMinutes(),
                maxP,
                activeCount,
                maxP - activeCount,
                pricePerPersonCents,
                invitation.getExpiresAt(),
                invitation.isExpired(),
                activeCount >= maxP,
                participants
        );
    }
}
