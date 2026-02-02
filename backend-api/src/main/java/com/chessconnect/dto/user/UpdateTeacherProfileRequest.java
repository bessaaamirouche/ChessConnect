package com.chessconnect.dto.user;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class UpdateTeacherProfileRequest {
    @Min(value = 50, message = "Le tarif minimum est de 0.50€ (limite Stripe)")
    private Integer hourlyRateCents;

    private String bio;
}
