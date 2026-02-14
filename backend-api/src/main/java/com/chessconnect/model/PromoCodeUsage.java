package com.chessconnect.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "promo_code_usages", indexes = {
    @Index(name = "idx_promo_code_usages_code", columnList = "promo_code_id"),
    @Index(name = "idx_promo_code_usages_user", columnList = "user_id")
})
public class PromoCodeUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promo_code_id", nullable = false)
    private PromoCode promoCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id")
    private Lesson lesson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @Column(name = "original_amount_cents", nullable = false)
    private Integer originalAmountCents;

    @Column(name = "discount_amount_cents")
    private Integer discountAmountCents = 0;

    @Column(name = "commission_saved_cents")
    private Integer commissionSavedCents = 0;

    @Column(name = "used_at", nullable = false, updatable = false)
    private LocalDateTime usedAt;

    @PrePersist
    protected void onCreate() {
        usedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public PromoCode getPromoCode() { return promoCode; }
    public void setPromoCode(PromoCode promoCode) { this.promoCode = promoCode; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Lesson getLesson() { return lesson; }
    public void setLesson(Lesson lesson) { this.lesson = lesson; }

    public Payment getPayment() { return payment; }
    public void setPayment(Payment payment) { this.payment = payment; }

    public Integer getOriginalAmountCents() { return originalAmountCents; }
    public void setOriginalAmountCents(Integer originalAmountCents) { this.originalAmountCents = originalAmountCents; }

    public Integer getDiscountAmountCents() { return discountAmountCents; }
    public void setDiscountAmountCents(Integer discountAmountCents) { this.discountAmountCents = discountAmountCents; }

    public Integer getCommissionSavedCents() { return commissionSavedCents; }
    public void setCommissionSavedCents(Integer commissionSavedCents) { this.commissionSavedCents = commissionSavedCents; }

    public LocalDateTime getUsedAt() { return usedAt; }
}
