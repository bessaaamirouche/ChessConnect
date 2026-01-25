package com.chessconnect.service;

import com.chessconnect.dto.exercise.ExerciseResponse;
import com.chessconnect.model.Course;
import com.chessconnect.model.Exercise;
import com.chessconnect.model.Lesson;
import com.chessconnect.model.Progress;
import com.chessconnect.model.UserCourseProgress;
import com.chessconnect.model.enums.ChessLevel;
import com.chessconnect.model.enums.CourseStatus;
import com.chessconnect.model.enums.DifficultyLevel;
import com.chessconnect.repository.CourseRepository;
import com.chessconnect.repository.ExerciseRepository;
import com.chessconnect.repository.LessonRepository;
import com.chessconnect.repository.ProgressRepository;
import com.chessconnect.repository.UserCourseProgressRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ExerciseService {

    private static final Logger log = LoggerFactory.getLogger(ExerciseService.class);

    private final ExerciseRepository exerciseRepository;
    private final LessonRepository lessonRepository;
    private final ProgressRepository progressRepository;
    private final SubscriptionService subscriptionService;
    private final UserCourseProgressRepository userCourseProgressRepository;
    private final CourseRepository courseRepository;

    // Map of course title keywords to starting FEN positions
    private static final Map<String, ExerciseConfig> COURSE_EXERCISE_CONFIGS = new HashMap<>();

    static {
        // Ouvertures - Niveau CAVALIER et FOU
        COURSE_EXERCISE_CONFIGS.put("ouverture italienne",
            new ExerciseConfig(
                "r1bqkbnr/pppp1ppp/2n5/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R b KQkq - 3 3",
                "Jouez l'ouverture Italienne. Developpez vos pieces vers le centre et preparez le roque.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("ouverture espagnole",
            new ExerciseConfig(
                "r1bqkbnr/pppp1ppp/2n5/1B2p3/4P3/5N2/PPPP1PPP/RNBQK2R b KQkq - 3 3",
                "Jouez la Ruy Lopez. Mettez la pression sur le cavalier c6 et preparez le centre.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("defense sicilienne",
            new ExerciseConfig(
                "rnbqkbnr/pp1ppppp/8/2p5/4P3/8/PPPP1PPP/RNBQKBNR w KQkq c6 0 2",
                "Affrontez la Sicilienne. Les Blancs ont plusieurs plans : Cf3, c3, ou f4.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("defense francaise",
            new ExerciseConfig(
                "rnbqkbnr/pppp1ppp/4p3/8/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2",
                "Jouez contre la Defense Francaise. Poussez e5 ou echangez en d5.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("gambit dame",
            new ExerciseConfig(
                "rnbqkbnr/ppp1pppp/8/3p4/2PP4/8/PP2PPPP/RNBQKBNR b KQkq c3 0 2",
                "Jouez le Gambit Dame. Controlez le centre et developpez harmonieusement.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("defense indienne",
            new ExerciseConfig(
                "rnbqkb1r/pppppppp/5n2/8/3P4/8/PPP1PPPP/RNBQKBNR w KQkq - 1 2",
                "Affrontez une defense Indienne. Developpez avec c4 et Cc3.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("caro-kann",
            new ExerciseConfig(
                "rnbqkbnr/pp1ppppp/2p5/8/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2",
                "Jouez contre la Caro-Kann. Une defense solide a combattre.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("defense slave",
            new ExerciseConfig(
                "rnbqkbnr/pp2pppp/2p5/3p4/2PP4/8/PP2PPPP/RNBQKBNR w KQkq - 0 3",
                "La Defense Slave. Solide et flexible pour les Noirs.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("nimzo-indienne",
            new ExerciseConfig(
                "rnbqk2r/pppp1ppp/4pn2/8/1bPP4/2N5/PP2PPPP/R1BQKBNR w KQkq - 2 4",
                "La Nimzo-Indienne. Les Noirs controlent e4 avec le Fou en b4.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("est-indienne",
            new ExerciseConfig(
                "rnbqk2r/ppp1ppbp/3p1np1/8/2PPP3/2N5/PP3PPP/R1BQKBNR w KQkq - 0 5",
                "L'Est-Indienne. Les Noirs preparent une attaque sur l'aile roi.",
                "white"));

        // Tactiques - Niveau CAVALIER
        COURSE_EXERCISE_CONFIGS.put("fourchette",
            new ExerciseConfig(
                "r1bqkb1r/pppp1ppp/2n2n2/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR w KQkq - 4 4",
                "Trouvez la fourchette ! Attaquez deux pieces en meme temps.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("clouage",
            new ExerciseConfig(
                "r1bqkbnr/pppp1ppp/2n5/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4",
                "Utilisez le clouage pour immobiliser une piece adverse.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("enfilade",
            new ExerciseConfig(
                "4k3/8/8/8/8/8/4R3/4K3 w - - 0 1",
                "Pratiquez l'enfilade : attaquez une piece qui protege une autre.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("mat en 2",
            new ExerciseConfig(
                "r1bqkb1r/pppp1Qpp/2n2n2/4p3/2B1P3/8/PPPP1PPP/RNB1K1NR b KQkq - 0 4",
                "Trouvez le mat en 2 coups !",
                "white"));

        // Finales - Niveau CAVALIER et FOU
        COURSE_EXERCISE_CONFIGS.put("finale roi + dame",
            new ExerciseConfig(
                "8/8/8/4k3/8/8/3QK3/8 w - - 0 1",
                "Matez avec Roi et Dame contre Roi seul. Poussez le Roi adverse vers le bord.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("finale roi + tour",
            new ExerciseConfig(
                "8/8/8/4k3/8/8/4K3/4R3 w - - 0 1",
                "Matez avec Roi et Tour. Utilisez la technique de l'escalier.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("finale roi + 2 fous",
            new ExerciseConfig(
                "8/8/8/4k3/8/8/2B1K3/3B4 w - - 0 1",
                "Matez avec deux Fous. Coordonnez vos Fous pour pousser le Roi dans le coin.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("lucena",
            new ExerciseConfig(
                "1K1k4/1P6/8/8/8/8/r7/4R3 w - - 0 1",
                "Position de Lucena : construisez le pont pour promouvoir le pion.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("philidor",
            new ExerciseConfig(
                "8/8/8/8/3k4/8/3KP3/r7 b - - 0 1",
                "Position de Philidor : defendez avec votre Tour sur la 6eme rangee.",
                "black"));
        COURSE_EXERCISE_CONFIGS.put("opposition",
            new ExerciseConfig(
                "8/8/8/3k4/8/3K4/3P4/8 w - - 0 1",
                "L'opposition : prenez l'opposition pour promouvoir votre pion.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("regle du carre",
            new ExerciseConfig(
                "8/8/8/8/P7/8/8/4k1K1 w - - 0 1",
                "La regle du carre : votre Roi peut-il rattraper le pion ?",
                "black"));

        // Bases - Niveau PION
        COURSE_EXERCISE_CONFIGS.put("mat du couloir",
            new ExerciseConfig(
                "6k1/5ppp/8/8/8/8/8/R3K3 w - - 0 1",
                "Le mat du couloir : profitez du Roi enferme derriere ses pions.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("mat du berger",
            new ExerciseConfig(
                "r1bqkbnr/pppp1ppp/2n5/4p3/2B1P3/5Q2/PPPP1PPP/RNB1K1NR b KQkq - 3 3",
                "Defendez-vous contre le mat du berger ! Protegez f7.",
                "black"));
        COURSE_EXERCISE_CONFIGS.put("controler le centre",
            new ExerciseConfig(
                "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1",
                "Controlez le centre avec vos pions et pieces.",
                "black"));
        COURSE_EXERCISE_CONFIGS.put("developper ses pieces",
            new ExerciseConfig(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "Developpez vos pieces rapidement vers des cases actives.",
                "white"));
    }

    public ExerciseService(
        ExerciseRepository exerciseRepository,
        LessonRepository lessonRepository,
        ProgressRepository progressRepository,
        SubscriptionService subscriptionService,
        UserCourseProgressRepository userCourseProgressRepository,
        CourseRepository courseRepository
    ) {
        this.exerciseRepository = exerciseRepository;
        this.lessonRepository = lessonRepository;
        this.progressRepository = progressRepository;
        this.subscriptionService = subscriptionService;
        this.userCourseProgressRepository = userCourseProgressRepository;
        this.courseRepository = courseRepository;
    }

    // Configuration class for exercises
    private static class ExerciseConfig {
        final String fen;
        final String tip;
        final String playerColor;

        ExerciseConfig(String fen, String tip, String playerColor) {
            this.fen = fen;
            this.tip = tip;
            this.playerColor = playerColor;
        }
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

        // Get current course (IN_PROGRESS or last COMPLETED) for the student
        Optional<Course> currentCourse = getCurrentCourseForStudent(userId, studentLevel);

        // Find matching exercise config based on course title
        ExerciseConfig config = null;
        String courseTitle = null;
        if (currentCourse.isPresent()) {
            courseTitle = currentCourse.get().getTitle();
            config = findExerciseConfigForCourse(courseTitle);
        }

        Exercise exercise = new Exercise();
        exercise.setLesson(lesson);

        if (config != null && courseTitle != null) {
            // Exercise linked to the current course
            exercise.setTitle(courseTitle);
            exercise.setDescription(config.tip);
            exercise.setStartingFen(config.fen);
            exercise.setPlayerColor(config.playerColor);
            log.info("Generated exercise linked to course '{}' for user {}", courseTitle, userId);
        } else {
            // Default exercise - standard position
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
            String lessonDate = lesson.getScheduledAt().format(formatter);
            exercise.setTitle("Cours du " + lessonDate + " avec " + lesson.getTeacher().getFirstName());

            // Use teacher observations as tips if available
            String description;
            if (lesson.getTeacherObservations() != null && !lesson.getTeacherObservations().isBlank()) {
                description = lesson.getTeacherObservations();
            } else {
                description = "Exercez-vous contre myChessBot pour mettre en pratique ce que vous avez appris. Niveau: " + studentLevel.getDisplayName();
            }
            exercise.setDescription(description);
            exercise.setStartingFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
            exercise.setPlayerColor("white");
        }

        exercise.setLesson(lesson);
        exercise.setDifficultyLevel(difficulty);
        exercise.setChessLevel(studentLevel);

        Exercise saved = exerciseRepository.save(exercise);
        log.info("Generated new exercise {} for lesson {}, difficulty={}, course={}",
            saved.getId(), lesson.getId(), difficulty, courseTitle);

        return saved;
    }

    /**
     * Get the current course (IN_PROGRESS) or the last completed course for the student
     */
    private Optional<Course> getCurrentCourseForStudent(Long userId, ChessLevel studentLevel) {
        List<UserCourseProgress> allProgress = userCourseProgressRepository.findByUserId(userId);

        // First, try to find a course IN_PROGRESS or PENDING_VALIDATION
        Optional<UserCourseProgress> inProgress = allProgress.stream()
            .filter(p -> p.getStatus() == CourseStatus.IN_PROGRESS || p.getStatus() == CourseStatus.PENDING_VALIDATION)
            .max(Comparator.comparing(p -> p.getStartedAt() != null ? p.getStartedAt() : java.time.LocalDateTime.MIN));

        if (inProgress.isPresent()) {
            return Optional.of(inProgress.get().getCourse());
        }

        // If no IN_PROGRESS, get the last COMPLETED course
        Optional<UserCourseProgress> lastCompleted = allProgress.stream()
            .filter(p -> p.getStatus() == CourseStatus.COMPLETED)
            .max(Comparator.comparing(p -> p.getCompletedAt() != null ? p.getCompletedAt() : java.time.LocalDateTime.MIN));

        if (lastCompleted.isPresent()) {
            return Optional.of(lastCompleted.get().getCourse());
        }

        // If no progress, get the first course of the student's level
        return courseRepository.findByGradeAndOrderInGrade(studentLevel, 1);
    }

    /**
     * Find exercise config that matches the course title
     */
    private ExerciseConfig findExerciseConfigForCourse(String courseTitle) {
        if (courseTitle == null) return null;

        String titleLower = courseTitle.toLowerCase();

        for (Map.Entry<String, ExerciseConfig> entry : COURSE_EXERCISE_CONFIGS.entrySet()) {
            if (titleLower.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        return null;
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
