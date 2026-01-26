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

    @Column(name = "pion_score")
    private Integer pionScore;

    @Column(name = "cavalier_score")
    private Integer cavalierScore;

    @Column(name = "fou_score")
    private Integer fouScore;

    @Column(name = "tour_score")
    private Integer tourScore;

    @Column(name = "dame_score")
    private Integer dameScore;

    @Column(name = "roi_score")
    private Integer roiScore;

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

    public Integer getPionScore() { return pionScore; }
    public void setPionScore(Integer pionScore) { this.pionScore = pionScore; }

    public Integer getCavalierScore() { return cavalierScore; }
    public void setCavalierScore(Integer cavalierScore) { this.cavalierScore = cavalierScore; }

    public Integer getFouScore() { return fouScore; }
    public void setFouScore(Integer fouScore) { this.fouScore = fouScore; }

    public Integer getTourScore() { return tourScore; }
    public void setTourScore(Integer tourScore) { this.tourScore = tourScore; }

    public Integer getDameScore() { return dameScore; }
    public void setDameScore(Integer dameScore) { this.dameScore = dameScore; }

    public Integer getRoiScore() { return roiScore; }
    public void setRoiScore(Integer roiScore) { this.roiScore = roiScore; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
