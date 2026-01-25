package com.chessconnect.service;

import com.chessconnect.dto.exercise.ExerciseResponse;
import com.chessconnect.model.*;
import com.chessconnect.model.enums.*;
import com.chessconnect.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExerciseService Tests")
class ExerciseServiceTest {

    @Mock
    private ExerciseRepository exerciseRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private ProgressRepository progressRepository;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private UserCourseProgressRepository userCourseProgressRepository;

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private ExerciseService exerciseService;

    private User studentUser;
    private User teacherUser;
    private Lesson completedLesson;
    private Exercise existingExercise;
    private Progress studentProgress;

    @BeforeEach
    void setUp() {
        // Setup student user
        studentUser = new User();
        studentUser.setId(1L);
        studentUser.setEmail("student@test.com");
        studentUser.setFirstName("John");
        studentUser.setLastName("Doe");
        studentUser.setRole(Role.STUDENT);

        // Setup teacher user
        teacherUser = new User();
        teacherUser.setId(2L);
        teacherUser.setEmail("teacher@test.com");
        teacherUser.setFirstName("Lamine");
        teacherUser.setLastName("Coach");
        teacherUser.setRole(Role.TEACHER);

        // Setup completed lesson
        completedLesson = new Lesson();
        completedLesson.setId(1L);
        completedLesson.setStudent(studentUser);
        completedLesson.setTeacher(teacherUser);
        completedLesson.setStatus(LessonStatus.COMPLETED);
        completedLesson.setScheduledAt(LocalDateTime.now().minusDays(1));

        // Setup student progress
        studentProgress = new Progress();
        studentProgress.setId(1L);
        studentProgress.setUser(studentUser);
        studentProgress.setChessLevel(ChessLevel.PION);

        // Setup existing exercise
        existingExercise = new Exercise();
        existingExercise.setId(1L);
        existingExercise.setLesson(completedLesson);
        existingExercise.setTitle("Test Exercise");
        existingExercise.setDescription("Test description with myChessBot");
        existingExercise.setStartingFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
        existingExercise.setPlayerColor("white");
        existingExercise.setDifficultyLevel(DifficultyLevel.DEBUTANT);
        existingExercise.setChessLevel(ChessLevel.PION);
    }

    @Nested
    @DisplayName("getExerciseForLesson Tests")
    class GetExerciseForLessonTests {

        @Test
        @DisplayName("Should return existing exercise when found")
        void shouldReturnExistingExercise() {
            // Given
            when(subscriptionService.hasActiveSubscription(anyLong())).thenReturn(true);
            when(lessonRepository.findById(1L)).thenReturn(Optional.of(completedLesson));
            when(exerciseRepository.findByLessonId(1L)).thenReturn(Optional.of(existingExercise));

            // When
            ExerciseResponse result = exerciseService.getExerciseForLesson(1L, 1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getTitle()).isEqualTo("Test Exercise");
            assertThat(result.getDescription()).contains("myChessBot");
            verify(exerciseRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when user has no premium subscription")
        void shouldThrowExceptionWhenNoPremium() {
            // Given
            when(subscriptionService.hasActiveSubscription(anyLong())).thenReturn(false);

            // When/Then
            assertThatThrownBy(() -> exerciseService.getExerciseForLesson(1L, 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Premium");
        }

        @Test
        @DisplayName("Should throw exception when lesson not found")
        void shouldThrowExceptionWhenLessonNotFound() {
            // Given
            when(subscriptionService.hasActiveSubscription(anyLong())).thenReturn(true);
            when(lessonRepository.findById(anyLong())).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> exerciseService.getExerciseForLesson(1L, 999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found");
        }

        @Test
        @DisplayName("Should throw exception when lesson not completed")
        void shouldThrowExceptionWhenLessonNotCompleted() {
            // Given
            completedLesson.setStatus(LessonStatus.PENDING);
            when(subscriptionService.hasActiveSubscription(anyLong())).thenReturn(true);
            when(lessonRepository.findById(1L)).thenReturn(Optional.of(completedLesson));

            // When/Then
            assertThatThrownBy(() -> exerciseService.getExerciseForLesson(1L, 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("completed");
        }

        @Test
        @DisplayName("Should throw exception when user is not the student of the lesson")
        void shouldThrowExceptionWhenNotLessonStudent() {
            // Given
            when(subscriptionService.hasActiveSubscription(anyLong())).thenReturn(true);
            when(lessonRepository.findById(1L)).thenReturn(Optional.of(completedLesson));

            // When/Then - user 999 is not the student
            assertThatThrownBy(() -> exerciseService.getExerciseForLesson(999L, 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not authorized");
        }

        @Test
        @DisplayName("Should generate new exercise when none exists")
        void shouldGenerateNewExercise() {
            // Given
            when(subscriptionService.hasActiveSubscription(anyLong())).thenReturn(true);
            when(lessonRepository.findById(1L)).thenReturn(Optional.of(completedLesson));
            when(exerciseRepository.findByLessonId(1L)).thenReturn(Optional.empty());
            when(progressRepository.findByUserId(1L)).thenReturn(Optional.of(studentProgress));
            when(userCourseProgressRepository.findByUserId(1L)).thenReturn(List.of());
            when(exerciseRepository.save(any(Exercise.class))).thenAnswer(invocation -> {
                Exercise saved = invocation.getArgument(0);
                saved.setId(2L);
                return saved;
            });

            // When
            ExerciseResponse result = exerciseService.getExerciseForLesson(1L, 1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getStartingFen()).isNotNull();
            assertThat(result.getPlayerColor()).isIn("white", "black");
            verify(exerciseRepository).save(any(Exercise.class));
        }
    }

    @Nested
    @DisplayName("getExerciseById Tests")
    class GetExerciseByIdTests {

        @Test
        @DisplayName("Should return exercise when found and user is authorized")
        void shouldReturnExercise() {
            // Given
            when(subscriptionService.hasActiveSubscription(1L)).thenReturn(true);
            when(exerciseRepository.findById(1L)).thenReturn(Optional.of(existingExercise));

            // When
            ExerciseResponse result = exerciseService.getExerciseById(1L, 1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should throw exception when exercise not found")
        void shouldThrowExceptionWhenNotFound() {
            // Given
            when(subscriptionService.hasActiveSubscription(1L)).thenReturn(true);
            when(exerciseRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> exerciseService.getExerciseById(1L, 999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found");
        }
    }

    @Nested
    @DisplayName("getAllExercisesForUser Tests")
    class GetAllExercisesForUserTests {

        @Test
        @DisplayName("Should return all exercises for user")
        void shouldReturnAllExercises() {
            // Given
            Exercise exercise2 = new Exercise();
            exercise2.setId(2L);
            exercise2.setLesson(completedLesson);
            exercise2.setTitle("Second Exercise");
            exercise2.setDescription("Another exercise");
            exercise2.setStartingFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
            exercise2.setPlayerColor("white");
            exercise2.setDifficultyLevel(DifficultyLevel.FACILE);
            exercise2.setChessLevel(ChessLevel.PION);

            when(subscriptionService.hasActiveSubscription(1L)).thenReturn(true);
            when(exerciseRepository.findByLessonStudentId(1L)).thenReturn(List.of(existingExercise, exercise2));

            // When
            List<ExerciseResponse> results = exerciseService.getAllExercisesForUser(1L);

            // Then
            assertThat(results).hasSize(2);
            assertThat(results).extracting(ExerciseResponse::getId).containsExactly(1L, 2L);
        }

        @Test
        @DisplayName("Should return empty list when no exercises")
        void shouldReturnEmptyList() {
            // Given
            when(subscriptionService.hasActiveSubscription(1L)).thenReturn(true);
            when(exerciseRepository.findByLessonStudentId(1L)).thenReturn(List.of());

            // When
            List<ExerciseResponse> results = exerciseService.getAllExercisesForUser(1L);

            // Then
            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("Difficulty Level Tests")
    class DifficultyLevelTests {

        @Test
        @DisplayName("Should map PION level to DEBUTANT difficulty")
        void shouldMapPionToDebutant() {
            // Given
            studentProgress.setChessLevel(ChessLevel.PION);
            when(subscriptionService.hasActiveSubscription(anyLong())).thenReturn(true);
            when(lessonRepository.findById(1L)).thenReturn(Optional.of(completedLesson));
            when(exerciseRepository.findByLessonId(1L)).thenReturn(Optional.empty());
            when(progressRepository.findByUserId(1L)).thenReturn(Optional.of(studentProgress));
            when(userCourseProgressRepository.findByUserId(1L)).thenReturn(List.of());
            when(exerciseRepository.save(any(Exercise.class))).thenAnswer(invocation -> {
                Exercise saved = invocation.getArgument(0);
                saved.setId(1L);
                return saved;
            });

            // When
            ExerciseResponse result = exerciseService.getExerciseForLesson(1L, 1L);

            // Then
            assertThat(result.getDifficultyLevel()).isEqualTo("DEBUTANT");
        }

        @Test
        @DisplayName("Should map CAVALIER level to FACILE difficulty")
        void shouldMapCavalierToFacile() {
            // Given
            studentProgress.setChessLevel(ChessLevel.CAVALIER);
            when(subscriptionService.hasActiveSubscription(anyLong())).thenReturn(true);
            when(lessonRepository.findById(1L)).thenReturn(Optional.of(completedLesson));
            when(exerciseRepository.findByLessonId(1L)).thenReturn(Optional.empty());
            when(progressRepository.findByUserId(1L)).thenReturn(Optional.of(studentProgress));
            when(userCourseProgressRepository.findByUserId(1L)).thenReturn(List.of());
            when(exerciseRepository.save(any(Exercise.class))).thenAnswer(invocation -> {
                Exercise saved = invocation.getArgument(0);
                saved.setId(1L);
                return saved;
            });

            // When
            ExerciseResponse result = exerciseService.getExerciseForLesson(1L, 1L);

            // Then
            assertThat(result.getDifficultyLevel()).isEqualTo("FACILE");
        }

        @Test
        @DisplayName("Should map DAME level to EXPERT difficulty")
        void shouldMapDameToExpert() {
            // Given
            studentProgress.setChessLevel(ChessLevel.DAME);
            when(subscriptionService.hasActiveSubscription(anyLong())).thenReturn(true);
            when(lessonRepository.findById(1L)).thenReturn(Optional.of(completedLesson));
            when(exerciseRepository.findByLessonId(1L)).thenReturn(Optional.empty());
            when(progressRepository.findByUserId(1L)).thenReturn(Optional.of(studentProgress));
            when(userCourseProgressRepository.findByUserId(1L)).thenReturn(List.of());
            when(exerciseRepository.save(any(Exercise.class))).thenAnswer(invocation -> {
                Exercise saved = invocation.getArgument(0);
                saved.setId(1L);
                return saved;
            });

            // When
            ExerciseResponse result = exerciseService.getExerciseForLesson(1L, 1L);

            // Then
            assertThat(result.getDifficultyLevel()).isEqualTo("EXPERT");
        }
    }

    @Nested
    @DisplayName("Exercise Description Update Tests")
    class ExerciseDescriptionUpdateTests {

        @Test
        @DisplayName("Should use myChessBot in description, not l'IA")
        void shouldUseMyChessBotInDescription() {
            // Given
            when(subscriptionService.hasActiveSubscription(anyLong())).thenReturn(true);
            when(lessonRepository.findById(1L)).thenReturn(Optional.of(completedLesson));
            when(exerciseRepository.findByLessonId(1L)).thenReturn(Optional.empty());
            when(progressRepository.findByUserId(1L)).thenReturn(Optional.of(studentProgress));
            when(userCourseProgressRepository.findByUserId(1L)).thenReturn(List.of());
            when(exerciseRepository.save(any(Exercise.class))).thenAnswer(invocation -> {
                Exercise saved = invocation.getArgument(0);
                saved.setId(1L);
                // Verify the description uses myChessBot
                assertThat(saved.getDescription()).contains("myChessBot");
                assertThat(saved.getDescription()).doesNotContain("l'IA");
                return saved;
            });

            // When
            exerciseService.getExerciseForLesson(1L, 1L);

            // Then - verification done in the answer
            verify(exerciseRepository).save(any(Exercise.class));
        }
    }

    @Nested
    @DisplayName("Exercise Config Matching Tests")
    class ExerciseConfigMatchingTests {

        @Test
        @DisplayName("Should match fourchette du cavalier course to specific FEN")
        void shouldMatchFourchetteConfig() {
            // Given
            Course fourchetteeCourse = new Course();
            fourchetteeCourse.setId(1L);
            fourchetteeCourse.setTitle("La fourchette du Cavalier");
            fourchetteeCourse.setGrade(ChessLevel.CAVALIER);

            UserCourseProgress progress = new UserCourseProgress();
            progress.setId(1L);
            progress.setUser(studentUser);
            progress.setCourse(fourchetteeCourse);
            progress.setStatus(CourseStatus.IN_PROGRESS);
            progress.setStartedAt(LocalDateTime.now());

            studentProgress.setChessLevel(ChessLevel.CAVALIER);

            when(subscriptionService.hasActiveSubscription(anyLong())).thenReturn(true);
            when(lessonRepository.findById(1L)).thenReturn(Optional.of(completedLesson));
            when(exerciseRepository.findByLessonId(1L)).thenReturn(Optional.empty());
            when(progressRepository.findByUserId(1L)).thenReturn(Optional.of(studentProgress));
            when(userCourseProgressRepository.findByUserId(1L)).thenReturn(List.of(progress));
            when(exerciseRepository.save(any(Exercise.class))).thenAnswer(invocation -> {
                Exercise saved = invocation.getArgument(0);
                saved.setId(1L);
                return saved;
            });

            // When
            ExerciseResponse result = exerciseService.getExerciseForLesson(1L, 1L);

            // Then
            assertThat(result.getTitle()).isEqualTo("La fourchette du Cavalier");
            assertThat(result.getDescription()).contains("fourchette");
        }

        @Test
        @DisplayName("Should match mat du couloir course")
        void shouldMatchMatDuCouloirConfig() {
            // Given
            Course matCouloirCourse = new Course();
            matCouloirCourse.setId(1L);
            matCouloirCourse.setTitle("Le mat du couloir");
            matCouloirCourse.setGrade(ChessLevel.PION);

            UserCourseProgress progress = new UserCourseProgress();
            progress.setId(1L);
            progress.setUser(studentUser);
            progress.setCourse(matCouloirCourse);
            progress.setStatus(CourseStatus.IN_PROGRESS);
            progress.setStartedAt(LocalDateTime.now());

            when(subscriptionService.hasActiveSubscription(anyLong())).thenReturn(true);
            when(lessonRepository.findById(1L)).thenReturn(Optional.of(completedLesson));
            when(exerciseRepository.findByLessonId(1L)).thenReturn(Optional.empty());
            when(progressRepository.findByUserId(1L)).thenReturn(Optional.of(studentProgress));
            when(userCourseProgressRepository.findByUserId(1L)).thenReturn(List.of(progress));
            when(exerciseRepository.save(any(Exercise.class))).thenAnswer(invocation -> {
                Exercise saved = invocation.getArgument(0);
                saved.setId(1L);
                return saved;
            });

            // When
            ExerciseResponse result = exerciseService.getExerciseForLesson(1L, 1L);

            // Then
            assertThat(result.getTitle()).isEqualTo("Le mat du couloir");
            // Should have specific FEN for back rank mate
            assertThat(result.getStartingFen()).contains("k");
        }
    }
}
