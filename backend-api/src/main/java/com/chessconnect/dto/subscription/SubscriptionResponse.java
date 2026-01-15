package com.chessconnect.dto.subscription;

import com.chessconnect.model.Subscription;
import com.chessconnect.model.enums.SubscriptionPlan;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class SubscriptionResponse {
    private Long id;
    private Long studentId;
    private String studentName;
    private SubscriptionPlan planType;
    private String planName;
    private Integer priceCents;
    private Integer monthlyQuota;
    private Integer lessonsUsedThisMonth;
    private Integer remainingLessons;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime cancelledAt;
    private Boolean isActive;
    private LocalDateTime createdAt;

    public static SubscriptionResponse fromEntity(Subscription subscription) {
        return SubscriptionResponse.builder()
                .id(subscription.getId())
                .studentId(subscription.getStudent().getId())
                .studentName(subscription.getStudent().getFullName())
                .planType(subscription.getPlanType())
                .planName(subscription.getPlanType().getDisplayName())
                .priceCents(subscription.getPriceCents())
                .monthlyQuota(subscription.getMonthlyQuota())
                .lessonsUsedThisMonth(subscription.getLessonsUsedThisMonth())
                .remainingLessons(subscription.getRemainingLessons())
                .startDate(subscription.getStartDate())
                .endDate(subscription.getEndDate())
                .cancelledAt(subscription.getCancelledAt())
                .isActive(subscription.getIsActive())
                .createdAt(subscription.getCreatedAt())
                .build();
    }
}
