package com.chessconnect.dto.payment;

import com.chessconnect.model.enums.SubscriptionPlan;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateCheckoutSessionRequest {
    @NotNull(message = "Plan is required")
    private SubscriptionPlan plan;
}
