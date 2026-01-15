package com.chessconnect.model;

import com.chessconnect.model.enums.ChessLevel;
import jakarta.persistence.*;

@Entity
@Table(name = "quiz_questions")
public class QuizQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChessLevel level;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(name = "option_a", nullable = false)
    private String optionA;

    @Column(name = "option_b", nullable = false)
    private String optionB;

    @Column(name = "option_c", nullable = false)
    private String optionC;

    @Column(name = "option_d")
    private String optionD;

    @Column(name = "correct_answer", nullable = false)
    private String correctAnswer;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "order_in_level")
    private Integer orderInLevel;

    // Constructors
    public QuizQuestion() {}

    public QuizQuestion(ChessLevel level, String question, String optionA, String optionB,
                        String optionC, String optionD, String correctAnswer,
                        String explanation, Integer orderInLevel) {
        this.level = level;
        this.question = question;
        this.optionA = optionA;
        this.optionB = optionB;
        this.optionC = optionC;
        this.optionD = optionD;
        this.correctAnswer = correctAnswer;
        this.explanation = explanation;
        this.orderInLevel = orderInLevel;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ChessLevel getLevel() { return level; }
    public void setLevel(ChessLevel level) { this.level = level; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public String getOptionA() { return optionA; }
    public void setOptionA(String optionA) { this.optionA = optionA; }

    public String getOptionB() { return optionB; }
    public void setOptionB(String optionB) { this.optionB = optionB; }

    public String getOptionC() { return optionC; }
    public void setOptionC(String optionC) { this.optionC = optionC; }

    public String getOptionD() { return optionD; }
    public void setOptionD(String optionD) { this.optionD = optionD; }

    public String getCorrectAnswer() { return correctAnswer; }
    public void setCorrectAnswer(String correctAnswer) { this.correctAnswer = correctAnswer; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }

    public Integer getOrderInLevel() { return orderInLevel; }
    public void setOrderInLevel(Integer orderInLevel) { this.orderInLevel = orderInLevel; }
}
