package com.chessconnect.model;

import com.chessconnect.model.enums.CourseStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_course_progress", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "course_id"})
})
public class UserCourseProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CourseStatus status = CourseStatus.LOCKED;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "validated_by_teacher_id")
    private Long validatedByTeacherId;

    @Column(name = "validated_at")
    private LocalDateTime validatedAt;

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

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }

    public CourseStatus getStatus() { return status; }
    public void setStatus(CourseStatus status) { this.status = status; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public Long getValidatedByTeacherId() { return validatedByTeacherId; }
    public void setValidatedByTeacherId(Long validatedByTeacherId) { this.validatedByTeacherId = validatedByTeacherId; }

    public LocalDateTime getValidatedAt() { return validatedAt; }
    public void setValidatedAt(LocalDateTime validatedAt) { this.validatedAt = validatedAt; }

    // Helper methods
    public void start() {
        this.status = CourseStatus.IN_PROGRESS;
        this.startedAt = LocalDateTime.now();
    }

    public void complete() {
        this.status = CourseStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void unlock() {
        if (this.status == CourseStatus.LOCKED) {
            this.status = CourseStatus.IN_PROGRESS;
            this.startedAt = LocalDateTime.now();
        }
    }

    public void submitForValidation() {
        this.status = CourseStatus.PENDING_VALIDATION;
    }

    public void validate(Long teacherId) {
        this.status = CourseStatus.COMPLETED;
        this.validatedByTeacherId = teacherId;
        this.validatedAt = LocalDateTime.now();
        this.completedAt = LocalDateTime.now();
    }
}
