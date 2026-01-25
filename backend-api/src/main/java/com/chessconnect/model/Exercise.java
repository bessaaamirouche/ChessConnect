package com.chessconnect.model;

import com.chessconnect.model.enums.ChessLevel;
import com.chessconnect.model.enums.DifficultyLevel;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "exercises", indexes = {
    @Index(name = "idx_exercise_lesson_id", columnList = "lesson_id"),
    @Index(name = "idx_exercise_chess_level", columnList = "chess_level")
})
public class Exercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id")
    private Lesson lesson;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "starting_fen", nullable = false)
    private String startingFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty_level", nullable = false)
    private DifficultyLevel difficultyLevel = DifficultyLevel.MOYEN;

    @Enumerated(EnumType.STRING)
    @Column(name = "chess_level")
    private ChessLevel chessLevel;

    @Column(name = "player_color", nullable = false)
    private String playerColor = "white";

    @Column(name = "time_limit_seconds")
    private Integer timeLimitSeconds;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Lesson getLesson() {
        return lesson;
    }

    public void setLesson(Lesson lesson) {
        this.lesson = lesson;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStartingFen() {
        return startingFen;
    }

    public void setStartingFen(String startingFen) {
        this.startingFen = startingFen;
    }

    public DifficultyLevel getDifficultyLevel() {
        return difficultyLevel;
    }

    public void setDifficultyLevel(DifficultyLevel difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }

    public ChessLevel getChessLevel() {
        return chessLevel;
    }

    public void setChessLevel(ChessLevel chessLevel) {
        this.chessLevel = chessLevel;
    }

    public String getPlayerColor() {
        return playerColor;
    }

    public void setPlayerColor(String playerColor) {
        this.playerColor = playerColor;
    }

    public Integer getTimeLimitSeconds() {
        return timeLimitSeconds;
    }

    public void setTimeLimitSeconds(Integer timeLimitSeconds) {
        this.timeLimitSeconds = timeLimitSeconds;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
