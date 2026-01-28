package com.chessconnect.dto.auth;

public record RegisterResponse(
    String email,
    String firstName,
    String message
) {}
