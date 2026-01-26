package com.chessconnect.service;

import com.chessconnect.dto.quiz.QuizAnswerRequest;
import com.chessconnect.dto.quiz.QuizQuestionResponse;
import com.chessconnect.dto.quiz.QuizResultResponse;
import com.chessconnect.dto.quiz.QuizSubmitRequest;
import com.chessconnect.model.Progress;
import com.chessconnect.model.QuizQuestion;
import com.chessconnect.model.QuizResult;
import com.chessconnect.model.User;
import com.chessconnect.model.enums.ChessLevel;
import com.chessconnect.repository.ProgressRepository;
import com.chessconnect.repository.QuizQuestionRepository;
import com.chessconnect.repository.QuizResultRepository;
import com.chessconnect.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class QuizService {

    private static final Logger log = LoggerFactory.getLogger(QuizService.class);
    private static final int PASSING_PERCENTAGE = 70;
    private static final int QUESTIONS_PER_LEVEL = 5;

    private final QuizQuestionRepository questionRepository;
    private final QuizResultRepository resultRepository;
    private final ProgressRepository progressRepository;
    private final UserRepository userRepository;

    public QuizService(
            QuizQuestionRepository questionRepository,
            QuizResultRepository resultRepository,
            ProgressRepository progressRepository,
            UserRepository userRepository
    ) {
        this.questionRepository = questionRepository;
        this.resultRepository = resultRepository;
        this.progressRepository = progressRepository;
        this.userRepository = userRepository;
    }

    /**
     * Get all quiz questions ordered by level and shuffled within each level.
     */
    public List<QuizQuestionResponse> getQuizQuestions() {
        List<QuizQuestion> allQuestions = questionRepository.findAllByOrderByLevelAscOrderInLevelAsc();

        // Group by level and shuffle within each level
        Map<ChessLevel, List<QuizQuestion>> byLevel = allQuestions.stream()
                .collect(Collectors.groupingBy(QuizQuestion::getLevel));

        List<QuizQuestionResponse> result = new ArrayList<>();

        for (ChessLevel level : ChessLevel.values()) {
            List<QuizQuestion> levelQuestions = byLevel.getOrDefault(level, List.of());
            Collections.shuffle(levelQuestions);
            levelQuestions.stream()
                    .map(QuizQuestionResponse::from)
                    .forEach(result::add);
        }

        return result;
    }

    /**
     * Evaluate the quiz and determine the student's level.
     * Algorithm: Start from PION, advance if score >= 70%
     */
    @Transactional
    public QuizResultResponse evaluateQuiz(Long studentId, QuizSubmitRequest request) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));

        // Get all questions to validate answers
        Map<Long, QuizQuestion> questionsById = questionRepository.findAll().stream()
                .collect(Collectors.toMap(QuizQuestion::getId, q -> q));

        // Calculate scores by level
        Map<ChessLevel, Integer> scoresByLevel = new EnumMap<>(ChessLevel.class);
        Map<ChessLevel, Integer> totalByLevel = new EnumMap<>(ChessLevel.class);

        // Initialize totals
        for (ChessLevel level : ChessLevel.values()) {
            scoresByLevel.put(level, 0);
            totalByLevel.put(level, (int) questionRepository.countByLevel(level));
        }

        // Count correct answers by level
        for (QuizAnswerRequest answer : request.answers()) {
            QuizQuestion question = questionsById.get(answer.questionId());
            if (question == null) continue;

            ChessLevel level = question.getLevel();
            if (answer.answer().equalsIgnoreCase(question.getCorrectAnswer())) {
                scoresByLevel.merge(level, 1, Integer::sum);
            }
        }

        // Determine level: highest level where score >= 70%
        ChessLevel determinedLevel = ChessLevel.PION;

        for (ChessLevel level : ChessLevel.values()) {
            int score = scoresByLevel.getOrDefault(level, 0);
            int total = totalByLevel.getOrDefault(level, QUESTIONS_PER_LEVEL);

            if (total == 0) continue;

            double percentage = (double) score / total * 100;

            if (percentage >= PASSING_PERCENTAGE) {
                determinedLevel = level;
            } else {
                // Stop at first level not passed
                break;
            }
        }

        // Create and save quiz result
        QuizResult quizResult = new QuizResult();
        quizResult.setStudent(student);
        quizResult.setDeterminedLevel(determinedLevel);
        quizResult.setPionScore(scoresByLevel.get(ChessLevel.PION));
        quizResult.setCavalierScore(scoresByLevel.get(ChessLevel.CAVALIER));
        quizResult.setFouScore(scoresByLevel.get(ChessLevel.FOU));
        quizResult.setTourScore(scoresByLevel.get(ChessLevel.TOUR));
        quizResult.setDameScore(scoresByLevel.get(ChessLevel.DAME));
        quizResult.setRoiScore(scoresByLevel.get(ChessLevel.ROI));

        resultRepository.save(quizResult);

        // Update student's progress level
        updateStudentLevel(studentId, determinedLevel);

        log.info("Quiz completed for student {}: determined level = {}", studentId, determinedLevel);

        return QuizResultResponse.from(quizResult, totalByLevel);
    }

    /**
     * Get the last quiz result for a student.
     */
    public Optional<QuizResultResponse> getLastResult(Long studentId) {
        return resultRepository.findTopByStudentIdOrderByCompletedAtDesc(studentId)
                .map(result -> {
                    Map<ChessLevel, Integer> totals = new EnumMap<>(ChessLevel.class);
                    for (ChessLevel level : ChessLevel.values()) {
                        totals.put(level, (int) questionRepository.countByLevel(level));
                    }
                    return QuizResultResponse.from(result, totals);
                });
    }

    /**
     * Update the student's progress to the determined level.
     */
    private void updateStudentLevel(Long studentId, ChessLevel newLevel) {
        Progress progress = progressRepository.findByStudentId(studentId)
                .orElse(null);

        if (progress == null) {
            // Create new progress if not exists
            User student = userRepository.findById(studentId).orElse(null);
            if (student != null) {
                progress = new Progress();
                progress.setStudent(student);
            }
        }

        if (progress != null) {
            progress.setCurrentLevel(newLevel);
            progress.setLessonsAtCurrentLevel(0);
            progress.setLessonsRequiredForNextLevel(calculateRequiredLessons(newLevel));
            progressRepository.save(progress);

            log.info("Updated progress for student {} to level {}", studentId, newLevel);
        }
    }

    private int calculateRequiredLessons(ChessLevel level) {
        return switch (level) {
            case PION -> 45;
            case CAVALIER -> 45;
            case FOU -> 45;
            case TOUR -> 45;
            case DAME -> 45;
            case ROI -> 0; // Max level, no more lessons required
        };
    }
}
