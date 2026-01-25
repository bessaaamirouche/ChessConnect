package com.chessconnect.dto.tracking;

import jakarta.validation.constraints.NotBlank;

public record PageViewRequest(
    @NotBlank(message = "Page URL is required")
    String pageUrl,

    String sessionId
) {}
