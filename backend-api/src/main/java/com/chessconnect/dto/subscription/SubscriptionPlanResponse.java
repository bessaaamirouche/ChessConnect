package com.chessconnect.dto.subscription;

import com.chessconnect.model.enums.SubscriptionPlan;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SubscriptionPlanResponse {
    private String code;
    private String name;
    private Integer priceCents;
    private List<String> features;
    private Boolean popular;

    public static SubscriptionPlanResponse fromEnum(SubscriptionPlan plan) {
        return SubscriptionPlanResponse.builder()
                .code(plan.name())
                .name(plan.getDisplayName())
                .priceCents(plan.getPriceCents())
                .features(plan.getFeatures())
                .popular(plan == SubscriptionPlan.PREMIUM)
                .build();
    }
}
