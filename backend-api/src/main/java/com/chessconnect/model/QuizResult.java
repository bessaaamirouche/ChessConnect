package com.chessconnect.model;

import com.chessconnect.model.enums.ChessLevel;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "quiz_results")
public class QuizResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Enumerated(EnumType.STRING)
    @Column(name = "determined_level", nullable = false)
    private ChessLevel determinedLevel;

    @Column(name = "level_a_score")
    private Integer levelAScore;

    @Column(name = "level_b_score")
    private Integer levelBScore;

    @Column(name = "level_c_score")
    private Integer levelCScore;

    @Column(name = "level_d_score")
    private Integer levelDScore;

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        completedAt = LocalDateTime.now();
    }

    // Constructors
    public QuizResult() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getStudent() { return student; }
    public void setStudent(User student) { this.student = student; }

    public ChessLevel getDeterminedLevel() { return determinedLevel; }
    public void setDeterminedLevel(ChessLevel determinedLevel) { this.determinedLevel = determinedLevel; }

    public Integer getLevelAScore() { return levelAScore; }
    public void setLevelAScore(Integer levelAScore) { this.levelAScore = levelAScore; }

    public Integer getLevelBScore() { return levelBScore; }
    public void setLevelBScore(Integer levelBScore) { this.levelBScore = levelBScore; }

    public Integer getLevelCScore() { return levelCScore; }
    public void setLevelCScore(Integer levelCScore) { this.levelCScore = levelCScore; }

    public Integer getLevelDScore() { return levelDScore; }
    public void setLevelDScore(Integer levelDScore) { this.levelDScore = levelDScore; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
