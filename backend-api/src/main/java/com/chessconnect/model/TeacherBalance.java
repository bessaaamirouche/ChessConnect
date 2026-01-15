package com.chessconnect.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "teacher_balances")
public class TeacherBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false, unique = true)
    private User teacher;

    @Column(name = "available_balance_cents", nullable = false)
    private Integer availableBalanceCents = 0;

    @Column(name = "pending_balance_cents", nullable = false)
    private Integer pendingBalanceCents = 0;

    @Column(name = "total_earned_cents", nullable = false)
    private Integer totalEarnedCents = 0;

    @Column(name = "total_withdrawn_cents", nullable = false)
    private Integer totalWithdrawnCents = 0;

    @Column(name = "lessons_completed", nullable = false)
    private Integer lessonsCompleted = 0;

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

    public void addEarnings(int amountCents) {
        this.availableBalanceCents += amountCents;
        this.totalEarnedCents += amountCents;
        this.lessonsCompleted++;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getTeacher() { return teacher; }
    public void setTeacher(User teacher) { this.teacher = teacher; }

    public Integer getAvailableBalanceCents() { return availableBalanceCents; }
    public void setAvailableBalanceCents(Integer availableBalanceCents) { this.availableBalanceCents = availableBalanceCents; }

    public Integer getPendingBalanceCents() { return pendingBalanceCents; }
    public void setPendingBalanceCents(Integer pendingBalanceCents) { this.pendingBalanceCents = pendingBalanceCents; }

    public Integer getTotalEarnedCents() { return totalEarnedCents; }
    public void setTotalEarnedCents(Integer totalEarnedCents) { this.totalEarnedCents = totalEarnedCents; }

    public Integer getTotalWithdrawnCents() { return totalWithdrawnCents; }
    public void setTotalWithdrawnCents(Integer totalWithdrawnCents) { this.totalWithdrawnCents = totalWithdrawnCents; }

    public Integer getLessonsCompleted() { return lessonsCompleted; }
    public void setLessonsCompleted(Integer lessonsCompleted) { this.lessonsCompleted = lessonsCompleted; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
