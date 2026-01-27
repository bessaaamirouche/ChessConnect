package com.chessconnect.dto.user;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class UpdateTeacherProfileRequest {
    @Min(value = 1000, message = "Le tarif minimum est de 10€")
    private Integer hourlyRateCents;

    private Boolean acceptsFreeTrial;

    private String bio;
}
