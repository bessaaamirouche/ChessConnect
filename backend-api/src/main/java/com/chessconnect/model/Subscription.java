package com.chessconnect.model;

import com.chessconnect.model.enums.SubscriptionPlan;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "subscriptions")
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_type", nullable = false)
    private SubscriptionPlan planType;

    @Column(name = "price_cents", nullable = false)
    private Integer priceCents;

    @Column(name = "monthly_quota", nullable = false)
    private Integer monthlyQuota;

    @Column(name = "lessons_used_this_month")
    private Integer lessonsUsedThisMonth = 0;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "stripe_subscription_id")
    private String stripeSubscriptionId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getStudent() { return student; }
    public void setStudent(User student) { this.student = student; }

    public SubscriptionPlan getPlanType() { return planType; }
    public void setPlanType(SubscriptionPlan planType) { this.planType = planType; }

    public Integer getPriceCents() { return priceCents; }
    public void setPriceCents(Integer priceCents) { this.priceCents = priceCents; }

    public Integer getMonthlyQuota() { return monthlyQuota; }
    public void setMonthlyQuota(Integer monthlyQuota) { this.monthlyQuota = monthlyQuota; }

    public Integer getLessonsUsedThisMonth() { return lessonsUsedThisMonth; }
    public void setLessonsUsedThisMonth(Integer lessonsUsedThisMonth) { this.lessonsUsedThisMonth = lessonsUsedThisMonth; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public String getStripeSubscriptionId() { return stripeSubscriptionId; }
    public void setStripeSubscriptionId(String stripeSubscriptionId) { this.stripeSubscriptionId = stripeSubscriptionId; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public boolean hasRemainingLessons() {
        return lessonsUsedThisMonth < monthlyQuota;
    }

    public int getRemainingLessons() {
        return monthlyQuota - lessonsUsedThisMonth;
    }
}
