package com.chessconnect.model;

import com.chessconnect.model.enums.DiscountType;
import com.chessconnect.model.enums.PromoCodeType;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "promo_codes", indexes = {
    @Index(name = "idx_promo_codes_code", columnList = "code"),
    @Index(name = "idx_promo_codes_type", columnList = "code_type"),
    @Index(name = "idx_promo_codes_active", columnList = "is_active")
})
public class PromoCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "code_type", nullable = false, length = 20)
    private PromoCodeType codeType;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", length = 30)
    private DiscountType discountType;

    @Column(name = "discount_percent")
    private Double discountPercent;

    @Column(name = "referrer_name")
    private String referrerName;

    @Column(name = "referrer_email")
    private String referrerEmail;

    @Column(name = "premium_days")
    private Integer premiumDays = 0;

    @Column(name = "revenue_share_percent")
    private Double revenueSharePercent = 0.0;

    @Column(name = "max_uses")
    private Integer maxUses;

    @Column(name = "current_uses")
    private Integer currentUses = 0;

    @Column(name = "first_lesson_only")
    private Boolean firstLessonOnly = false;

    @Column(name = "min_amount_cents")
    private Integer minAmountCents;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean hasReachedMaxUses() {
        return maxUses != null && currentUses >= maxUses;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public PromoCodeType getCodeType() { return codeType; }
    public void setCodeType(PromoCodeType codeType) { this.codeType = codeType; }

    public DiscountType getDiscountType() { return discountType; }
    public void setDiscountType(DiscountType discountType) { this.discountType = discountType; }

    public Double getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(Double discountPercent) { this.discountPercent = discountPercent; }

    public String getReferrerName() { return referrerName; }
    public void setReferrerName(String referrerName) { this.referrerName = referrerName; }

    public String getReferrerEmail() { return referrerEmail; }
    public void setReferrerEmail(String referrerEmail) { this.referrerEmail = referrerEmail; }

    public Integer getPremiumDays() { return premiumDays; }
    public void setPremiumDays(Integer premiumDays) { this.premiumDays = premiumDays; }

    public Double getRevenueSharePercent() { return revenueSharePercent; }
    public void setRevenueSharePercent(Double revenueSharePercent) { this.revenueSharePercent = revenueSharePercent; }

    public Integer getMaxUses() { return maxUses; }
    public void setMaxUses(Integer maxUses) { this.maxUses = maxUses; }

    public Integer getCurrentUses() { return currentUses; }
    public void setCurrentUses(Integer currentUses) { this.currentUses = currentUses; }

    public Boolean getFirstLessonOnly() { return firstLessonOnly; }
    public void setFirstLessonOnly(Boolean firstLessonOnly) { this.firstLessonOnly = firstLessonOnly; }

    public Integer getMinAmountCents() { return minAmountCents; }
    public void setMinAmountCents(Integer minAmountCents) { this.minAmountCents = minAmountCents; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
