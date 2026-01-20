package com.chessconnect.dto.contact;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ContactAdminRequest(
        @NotBlank(message = "Le nom est obligatoire")
        @Size(min = 2, max = 100)
        String name,

        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "Email invalide")
        String email,

        @NotBlank(message = "Le sujet est obligatoire")
        @Size(min = 5, max = 200)
        String subject,

        @NotBlank(message = "Le message est obligatoire")
        @Size(min = 10, max = 2000)
        String message
) {}
