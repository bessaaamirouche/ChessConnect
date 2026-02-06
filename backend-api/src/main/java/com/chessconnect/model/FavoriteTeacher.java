package com.chessconnect.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "favorite_teachers", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"student_id", "teacher_id"})
})
public class FavoriteTeacher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    @Column(name = "notify_new_slots", nullable = false)
    private Boolean notifyNewSlots = true;

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

    public User getTeacher() { return teacher; }
    public void setTeacher(User teacher) { this.teacher = teacher; }

    public Boolean getNotifyNewSlots() { return notifyNewSlots; }
    public void setNotifyNewSlots(Boolean notifyNewSlots) { this.notifyNewSlots = notifyNewSlots; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
