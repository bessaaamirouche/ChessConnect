package com.chessconnect.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "referral_earnings", indexes = {
    @Index(name = "idx_referral_earnings_code", columnList = "promo_code_id"),
    @Index(name = "idx_referral_earnings_user", columnList = "referred_user_id")
})
public class ReferralEarning {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promo_code_id", nullable = false)
    private PromoCode promoCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referred_user_id", nullable = false)
    private User referredUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id")
    private Lesson lesson;

    @Column(name = "lesson_amount_cents", nullable = false)
    private Integer lessonAmountCents;

    @Column(name = "platform_commission_cents", nullable = false)
    private Integer platformCommissionCents;

    @Column(name = "referrer_earning_cents", nullable = false)
    private Integer referrerEarningCents;

    @Column(name = "is_paid")
    private Boolean isPaid = false;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "payment_reference")
    private String paymentReference;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public PromoCode getPromoCode() { return promoCode; }
    public void setPromoCode(PromoCode promoCode) { this.promoCode = promoCode; }

    public User getReferredUser() { return referredUser; }
    public void setReferredUser(User referredUser) { this.referredUser = referredUser; }

    public Lesson getLesson() { return lesson; }
    public void setLesson(Lesson lesson) { this.lesson = lesson; }

    public Integer getLessonAmountCents() { return lessonAmountCents; }
    public void setLessonAmountCents(Integer lessonAmountCents) { this.lessonAmountCents = lessonAmountCents; }

    public Integer getPlatformCommissionCents() { return platformCommissionCents; }
    public void setPlatformCommissionCents(Integer platformCommissionCents) { this.platformCommissionCents = platformCommissionCents; }

    public Integer getReferrerEarningCents() { return referrerEarningCents; }
    public void setReferrerEarningCents(Integer referrerEarningCents) { this.referrerEarningCents = referrerEarningCents; }

    public Boolean getIsPaid() { return isPaid; }
    public void setIsPaid(Boolean isPaid) { this.isPaid = isPaid; }

    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }

    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
