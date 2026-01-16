package com.chessconnect.dto.favorite;

import jakarta.validation.constraints.NotNull;

public record UpdateNotifyRequest(
    @NotNull(message = "notifyNewSlots is required")
    Boolean notifyNewSlots
) {}
