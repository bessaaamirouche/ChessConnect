package com.chessconnect.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "lesson_participants",
    uniqueConstraints = @UniqueConstraint(columnNames = {"lesson_id", "student_id"}),
    indexes = {
        @Index(name = "idx_lp_lesson", columnList = "lesson_id"),
        @Index(name = "idx_lp_student", columnList = "student_id"),
        @Index(name = "idx_lp_status", columnList = "status")
    }
)
public class LessonParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(nullable = false)
    private String role = "PARTICIPANT"; // CREATOR, PARTICIPANT

    @Column(nullable = false)
    private String status = "ACTIVE"; // ACTIVE, CANCELLED

    @Column(name = "price_paid_cents", nullable = false)
    private Integer pricePaidCents;

    @Column(name = "commission_cents", nullable = false)
    private Integer commissionCents;

    @Column(name = "cancelled_by")
    private String cancelledBy;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "refund_percentage")
    private Integer refundPercentage;

    @Column(name = "refunded_amount_cents")
    private Integer refundedAmountCents;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Lesson getLesson() { return lesson; }
    public void setLesson(Lesson lesson) { this.lesson = lesson; }

    public User getStudent() { return student; }
    public void setStudent(User student) { this.student = student; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getPricePaidCents() { return pricePaidCents; }
    public void setPricePaidCents(Integer pricePaidCents) { this.pricePaidCents = pricePaidCents; }

    public Integer getCommissionCents() { return commissionCents; }
    public void setCommissionCents(Integer commissionCents) { this.commissionCents = commissionCents; }

    public String getCancelledBy() { return cancelledBy; }
    public void setCancelledBy(String cancelledBy) { this.cancelledBy = cancelledBy; }

    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }

    public Integer getRefundPercentage() { return refundPercentage; }
    public void setRefundPercentage(Integer refundPercentage) { this.refundPercentage = refundPercentage; }

    public Integer getRefundedAmountCents() { return refundedAmountCents; }
    public void setRefundedAmountCents(Integer refundedAmountCents) { this.refundedAmountCents = refundedAmountCents; }

    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
