package com.chessconnect.dto.teacher;

import com.chessconnect.model.TeacherBalance;

public record TeacherBalanceResponse(
    Long teacherId,
    Integer availableBalanceCents,
    Integer pendingBalanceCents,
    Integer totalEarnedCents,
    Integer totalWithdrawnCents,
    Integer lessonsCompleted
) {
    public static TeacherBalanceResponse from(TeacherBalance balance) {
        return new TeacherBalanceResponse(
            balance.getTeacher().getId(),
            balance.getAvailableBalanceCents(),
            balance.getPendingBalanceCents(),
            balance.getTotalEarnedCents(),
            balance.getTotalWithdrawnCents(),
            balance.getLessonsCompleted()
        );
    }

    public static TeacherBalanceResponse empty(Long teacherId) {
        return new TeacherBalanceResponse(teacherId, 0, 0, 0, 0, 0);
    }
}
