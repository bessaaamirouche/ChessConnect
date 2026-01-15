package com.chessconnect.dto.auth;

import com.chessconnect.model.enums.UserRole;

public record AuthResponse(
        String token,
        Long userId,
        String email,
        String firstName,
        String lastName,
        UserRole role
) {}
