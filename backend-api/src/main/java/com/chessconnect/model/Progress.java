package com.chessconnect.model;

import com.chessconnect.model.enums.ChessLevel;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "progress_tracking")
public class Progress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false, unique = true)
    private User student;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_level", nullable = false)
    private ChessLevel currentLevel = ChessLevel.PION;

    @Column(name = "total_lessons_completed", nullable = false)
    private Integer totalLessonsCompleted = 0;

    @Column(name = "lessons_at_current_level", nullable = false)
    private Integer lessonsAtCurrentLevel = 0;

    @Column(name = "lessons_required_for_next_level", nullable = false)
    private Integer lessonsRequiredForNextLevel = 10;

    @Column(name = "last_lesson_date")
    private LocalDateTime lastLessonDate;

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

    public User getStudent() { return student; }
    public void setStudent(User student) { this.student = student; }

    public ChessLevel getCurrentLevel() { return currentLevel; }
    public void setCurrentLevel(ChessLevel currentLevel) { this.currentLevel = currentLevel; }

    public Integer getTotalLessonsCompleted() { return totalLessonsCompleted; }
    public void setTotalLessonsCompleted(Integer totalLessonsCompleted) { this.totalLessonsCompleted = totalLessonsCompleted; }

    public Integer getLessonsAtCurrentLevel() { return lessonsAtCurrentLevel; }
    public void setLessonsAtCurrentLevel(Integer lessonsAtCurrentLevel) { this.lessonsAtCurrentLevel = lessonsAtCurrentLevel; }

    public Integer getLessonsRequiredForNextLevel() { return lessonsRequiredForNextLevel; }
    public void setLessonsRequiredForNextLevel(Integer lessonsRequiredForNextLevel) { this.lessonsRequiredForNextLevel = lessonsRequiredForNextLevel; }

    public LocalDateTime getLastLessonDate() { return lastLessonDate; }
    public void setLastLessonDate(LocalDateTime lastLessonDate) { this.lastLessonDate = lastLessonDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void recordCompletedLesson() {
        this.totalLessonsCompleted++;
        this.lessonsAtCurrentLevel++;
        this.lastLessonDate = LocalDateTime.now();

        if (lessonsAtCurrentLevel >= lessonsRequiredForNextLevel && currentLevel != ChessLevel.DAME) {
            levelUp();
        }
    }

    private void levelUp() {
        this.currentLevel = currentLevel.nextLevel();
        this.lessonsAtCurrentLevel = 0;
        this.lessonsRequiredForNextLevel = calculateRequiredLessons(currentLevel);
    }

    private int calculateRequiredLessons(ChessLevel level) {
        return switch (level) {
            case PION -> 10;
            case CAVALIER -> 15;
            case FOU -> 20;
            case TOUR -> 25;
            case DAME -> Integer.MAX_VALUE;
        };
    }

    public double getProgressPercentage() {
        if (currentLevel == ChessLevel.DAME) return 100.0;
        return (double) lessonsAtCurrentLevel / lessonsRequiredForNextLevel * 100;
    }
}
