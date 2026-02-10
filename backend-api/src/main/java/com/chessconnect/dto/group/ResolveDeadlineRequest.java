package com.chessconnect.dto.group;

import jakarta.validation.constraints.NotNull;

public record ResolveDeadlineRequest(
    @NotNull String choice // "PAY_FULL" or "CANCEL"
) {}
