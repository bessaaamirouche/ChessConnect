package com.chessconnect.service;

import com.chessconnect.dto.exercise.ExerciseResponse;
import com.chessconnect.model.Exercise;
import com.chessconnect.model.Lesson;
import com.chessconnect.model.Progress;
import com.chessconnect.model.enums.ChessLevel;
import com.chessconnect.model.enums.DifficultyLevel;
import com.chessconnect.repository.ExerciseRepository;
import com.chessconnect.repository.LessonRepository;
import com.chessconnect.repository.ProgressRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ExerciseService {

    private static final Logger log = LoggerFactory.getLogger(ExerciseService.class);

    private final ExerciseRepository exerciseRepository;
    private final LessonRepository lessonRepository;
    private final ProgressRepository progressRepository;
    private final SubscriptionService subscriptionService;

    public ExerciseService(
        ExerciseRepository exerciseRepository,
        LessonRepository lessonRepository,
        ProgressRepository progressRepository,
        SubscriptionService subscriptionService
    ) {
        this.exerciseRepository = exerciseRepository;
        this.lessonRepository = lessonRepository;
        this.progressRepository = progressRepository;
        this.subscriptionService = subscriptionService;
    }

    public ExerciseResponse getExerciseForLesson(Long lessonId, Long userId) {
        // Check premium access
        if (!subscriptionService.isPremium(userId)) {
            throw new IllegalArgumentException("Un abonnement Premium est requis pour acceder aux exercices");
        }

        // Verify the lesson belongs to this user
        Lesson lesson = lessonRepository.findById(lessonId)
            .orElseThrow(() -> new IllegalArgumentException("Cours non trouve"));

        if (!lesson.getStudent().getId().equals(userId)) {
            throw new IllegalArgumentException("Vous n'avez pas acces a ce cours");
        }

        // Find or generate exercise for this lesson
        Exercise exercise = exerciseRepository.findByLessonId(lessonId)
            .orElseGet(() -> generateExerciseForLesson(lesson, userId));

        log.info("Exercise loaded for lesson {}, user {}: difficulty={}",
            lessonId, userId, exercise.getDifficultyLevel());

        return ExerciseResponse.from(exercise);
    }

    @Transactional
    private Exercise generateExerciseForLesson(Lesson lesson, Long userId) {
        // Determine difficulty based on student level
        ChessLevel studentLevel = progressRepository.findByStudentId(userId)
            .map(Progress::getCurrentLevel)
            .orElse(ChessLevel.PION);

        DifficultyLevel difficulty = mapChessLevelToDifficulty(studentLevel);

        // Format lesson date
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String lessonDate = lesson.getScheduledAt().format(formatter);

        Exercise exercise = new Exercise();
        exercise.setLesson(lesson);
        exercise.setTitle("Cours du " + lessonDate + " avec " + lesson.getTeacher().getFirstName());

        // Use teacher observations as tips if available
        String description;
        if (lesson.getTeacherObservations() != null && !lesson.getTeacherObservations().isBlank()) {
            description = lesson.getTeacherObservations();
        } else {
            description = "Exercez-vous contre l'IA pour mettre en pratique ce que vous avez appris. Niveau: " + studentLevel.getDisplayName();
        }
        exercise.setDescription(description);

        exercise.setStartingFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
        exercise.setDifficultyLevel(difficulty);
        exercise.setChessLevel(studentLevel);
        exercise.setPlayerColor("white");

        Exercise saved = exerciseRepository.save(exercise);
        log.info("Generated new exercise {} for lesson {}, difficulty={}",
            saved.getId(), lesson.getId(), difficulty);

        return saved;
    }

    private DifficultyLevel mapChessLevelToDifficulty(ChessLevel chessLevel) {
        return switch (chessLevel) {
            case PION -> DifficultyLevel.DEBUTANT;
            case CAVALIER -> DifficultyLevel.FACILE;
            case FOU -> DifficultyLevel.MOYEN;
            case TOUR -> DifficultyLevel.DIFFICILE;
            case DAME -> DifficultyLevel.EXPERT;
        };
    }

    public List<ExerciseResponse> getAllExercisesForUser(Long userId) {
        // Check premium access
        if (!subscriptionService.isPremium(userId)) {
            throw new IllegalArgumentException("Un abonnement Premium est requis pour acceder aux exercices");
        }

        // Get student's level for default exercises
        ChessLevel studentLevel = progressRepository.findByStudentId(userId)
            .map(Progress::getCurrentLevel)
            .orElse(ChessLevel.PION);

        return exerciseRepository.findByChessLevelOrderByCreatedAtDesc(studentLevel)
            .stream()
            .map(ExerciseResponse::from)
            .toList();
    }

    public ExerciseResponse getExerciseById(Long exerciseId, Long userId) {
        // Check premium access
        if (!subscriptionService.isPremium(userId)) {
            throw new IllegalArgumentException("Un abonnement Premium est requis pour acceder aux exercices");
        }

        Exercise exercise = exerciseRepository.findById(exerciseId)
            .orElseThrow(() -> new IllegalArgumentException("Exercice non trouve"));

        return ExerciseResponse.from(exercise);
    }
}
