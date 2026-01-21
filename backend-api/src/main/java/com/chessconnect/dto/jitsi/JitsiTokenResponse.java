package com.chessconnect.dto.jitsi;

public record JitsiTokenResponse(
    String token,
    String roomName,
    String domain,
    boolean isModerator
) {}
