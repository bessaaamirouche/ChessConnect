package com.chessconnect.dto.rating;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateRatingRequest(
    @NotNull(message = "Lesson ID is required")
    Long lessonId,

    @NotNull(message = "Stars rating is required")
    @Min(value = 1, message = "Rating must be at least 1 star")
    @Max(value = 5, message = "Rating cannot exceed 5 stars")
    Integer stars,

    String comment
) {}
