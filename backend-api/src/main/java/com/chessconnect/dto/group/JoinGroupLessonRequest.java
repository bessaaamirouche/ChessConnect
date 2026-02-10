package com.chessconnect.dto.group;

import jakarta.validation.constraints.NotNull;

public record JoinGroupLessonRequest(
    @NotNull String token,
    boolean useWalletCredit,
    Long courseId
) {}
