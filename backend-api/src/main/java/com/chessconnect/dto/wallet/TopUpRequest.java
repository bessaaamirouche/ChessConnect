package com.chessconnect.dto.wallet;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record TopUpRequest(
        @NotNull(message = "Amount is required")
        @Min(value = 500, message = "Minimum top-up amount is 5€")
        Integer amountCents,

        boolean embedded
) {
    public TopUpRequest {
        if (embedded == false) {
            embedded = false;
        }
    }
}
