package com.chessconnect.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Entity
@Table(name = "teacher_payouts")
public class TeacherPayout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    @Column(name = "year_month", nullable = false)
    private String yearMonth; // Format: "2026-01"

    @Column(name = "amount_cents", nullable = false)
    private Integer amountCents;

    @Column(name = "lessons_count", nullable = false)
    private Integer lessonsCount;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "is_paid", nullable = false)
    private Boolean isPaid = false;

    @Column(name = "payment_reference")
    private String paymentReference;

    @Column(name = "notes")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getTeacher() { return teacher; }
    public void setTeacher(User teacher) { this.teacher = teacher; }

    public String getYearMonth() { return yearMonth; }
    public void setYearMonth(String yearMonth) { this.yearMonth = yearMonth; }

    public Integer getAmountCents() { return amountCents; }
    public void setAmountCents(Integer amountCents) { this.amountCents = amountCents; }

    public Integer getLessonsCount() { return lessonsCount; }
    public void setLessonsCount(Integer lessonsCount) { this.lessonsCount = lessonsCount; }

    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }

    public Boolean getIsPaid() { return isPaid; }
    public void setIsPaid(Boolean isPaid) { this.isPaid = isPaid; }

    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
