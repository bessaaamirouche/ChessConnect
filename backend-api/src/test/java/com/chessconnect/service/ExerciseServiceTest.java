package com.chessconnect.service;

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
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
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

    @BeforeEach
    void setUp() {
        // Setup student user
        studentUser = new User();
        studentUser.setId(1L);
        studentUser.setEmail("student@test.com");
        studentUser.setFirstName("John");
        studentUser.setLastName("Doe");
        studentUser.setRole(UserRole.STUDENT);

        // Setup teacher user
        teacherUser = new User();
        teacherUser.setId(2L);
        teacherUser.setEmail("teacher@test.com");
        teacherUser.setFirstName("Lamine");
        teacherUser.setLastName("Coach");
        teacherUser.setRole(UserRole.TEACHER);

        // Setup completed lesson
        completedLesson = new Lesson();
        completedLesson.setId(1L);
        completedLesson.setStudent(studentUser);
        completedLesson.setTeacher(teacherUser);
        completedLesson.setStatus(LessonStatus.COMPLETED);
        completedLesson.setScheduledAt(LocalDateTime.now().minusDays(1));

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
    }

    @Nested
    @DisplayName("getExerciseById Tests")
    class GetExerciseByIdTests {

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
    @DisplayName("Difficulty Level Mapping Tests")
    class DifficultyLevelTests {

        @Test
        @DisplayName("DEBUTANT should have skill level 0")
        void debutantShouldHaveSkillLevel0() {
            assertThat(DifficultyLevel.DEBUTANT.getStockfishSkillLevel()).isEqualTo(0);
        }

        @Test
        @DisplayName("FACILE should have skill level 5")
        void facileShouldHaveSkillLevel5() {
            assertThat(DifficultyLevel.FACILE.getStockfishSkillLevel()).isEqualTo(5);
        }

        @Test
        @DisplayName("MOYEN should have skill level 10")
        void moyenShouldHaveSkillLevel10() {
            assertThat(DifficultyLevel.MOYEN.getStockfishSkillLevel()).isEqualTo(10);
        }

        @Test
        @DisplayName("DIFFICILE should have skill level 15")
        void difficileShouldHaveSkillLevel15() {
            assertThat(DifficultyLevel.DIFFICILE.getStockfishSkillLevel()).isEqualTo(15);
        }

        @Test
        @DisplayName("EXPERT should have skill level 20")
        void expertShouldHaveSkillLevel20() {
            assertThat(DifficultyLevel.EXPERT.getStockfishSkillLevel()).isEqualTo(20);
        }
    }

    @Nested
    @DisplayName("Chess Level Tests")
    class ChessLevelTests {

        @Test
        @DisplayName("All chess levels should exist")
        void allChessLevelsShouldExist() {
            assertThat(ChessLevel.PION).isNotNull();
            assertThat(ChessLevel.CAVALIER).isNotNull();
            assertThat(ChessLevel.FOU).isNotNull();
            assertThat(ChessLevel.TOUR).isNotNull();
            assertThat(ChessLevel.DAME).isNotNull();
        }
    }

    @Nested
    @DisplayName("Exercise Model Tests")
    class ExerciseModelTests {

        @Test
        @DisplayName("Exercise should have valid FEN")
        void exerciseShouldHaveValidFen() {
            assertThat(existingExercise.getStartingFen()).isNotNull();
            assertThat(existingExercise.getStartingFen()).contains("/");
        }

        @Test
        @DisplayName("Exercise should have valid player color")
        void exerciseShouldHaveValidPlayerColor() {
            assertThat(existingExercise.getPlayerColor()).isIn("white", "black");
        }

        @Test
        @DisplayName("Exercise description should use myChessBot, not l'IA")
        void exerciseDescriptionShouldUseMyChessBot() {
            assertThat(existingExercise.getDescription()).contains("myChessBot");
            assertThat(existingExercise.getDescription()).doesNotContain("l'IA");
        }
    }
}
