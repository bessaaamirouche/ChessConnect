package com.chessconnect.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record AdminLoginRequest(
        @NotBlank(message = "Identifiant requis")
        String username,

        @NotBlank(message = "Mot de passe requis")
        String password
) {}
