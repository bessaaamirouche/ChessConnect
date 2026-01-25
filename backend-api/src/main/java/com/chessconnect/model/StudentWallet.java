package com.chessconnect.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "student_wallets")
public class StudentWallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "balance_cents", nullable = false)
    private Integer balanceCents = 0;

    @Column(name = "total_top_ups_cents", nullable = false)
    private Integer totalTopUpsCents = 0;

    @Column(name = "total_used_cents", nullable = false)
    private Integer totalUsedCents = 0;

    @Column(name = "total_refunded_cents", nullable = false)
    private Integer totalRefundedCents = 0;

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

    public void addCredit(int amountCents) {
        this.balanceCents += amountCents;
        this.totalTopUpsCents += amountCents;
    }

    public void deductCredit(int amountCents) {
        if (this.balanceCents < amountCents) {
            throw new IllegalArgumentException("Insufficient credit balance");
        }
        this.balanceCents -= amountCents;
        this.totalUsedCents += amountCents;
    }

    public void refundCredit(int amountCents) {
        this.balanceCents += amountCents;
        this.totalRefundedCents += amountCents;
    }

    public boolean hasEnoughCredit(int amountCents) {
        return this.balanceCents >= amountCents;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Integer getBalanceCents() { return balanceCents; }
    public void setBalanceCents(Integer balanceCents) { this.balanceCents = balanceCents; }

    public Integer getTotalTopUpsCents() { return totalTopUpsCents; }
    public void setTotalTopUpsCents(Integer totalTopUpsCents) { this.totalTopUpsCents = totalTopUpsCents; }

    public Integer getTotalUsedCents() { return totalUsedCents; }
    public void setTotalUsedCents(Integer totalUsedCents) { this.totalUsedCents = totalUsedCents; }

    public Integer getTotalRefundedCents() { return totalRefundedCents; }
    public void setTotalRefundedCents(Integer totalRefundedCents) { this.totalRefundedCents = totalRefundedCents; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
