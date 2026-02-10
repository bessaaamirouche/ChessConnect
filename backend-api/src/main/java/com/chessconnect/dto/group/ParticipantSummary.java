package com.chessconnect.dto.group;

import com.chessconnect.model.LessonParticipant;

public record ParticipantSummary(
    String displayName,
    String role
) {
    public static ParticipantSummary from(LessonParticipant participant) {
        return new ParticipantSummary(
            participant.getStudent().getDisplayName(),
            participant.getRole()
        );
    }
}
