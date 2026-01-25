package com.chessconnect.controller;

import com.chessconnect.dto.exercise.ExerciseResponse;
import com.chessconnect.model.User;
import com.chessconnect.model.enums.Role;
import com.chessconnect.security.JwtService;
import com.chessconnect.service.ExerciseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ExerciseController.class)
@DisplayName("ExerciseController Tests")
class ExerciseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ExerciseService exerciseService;

    @MockBean
    private JwtService jwtService;

    private ExerciseResponse exerciseResponse;

    @BeforeEach
    void setUp() {
        exerciseResponse = ExerciseResponse.builder()
            .id(1L)
            .lessonId(1L)
            .title("Test Exercise")
            .description("Practice with myChessBot")
            .startingFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
            .playerColor("white")
            .difficultyLevel("DEBUTANT")
            .chessLevel("PION")
            .build();
    }

    @Nested
    @DisplayName("GET /api/exercises/lesson/{lessonId}")
    class GetExerciseForLessonTests {

        @Test
        @WithMockUser(roles = "STUDENT")
        @DisplayName("Should return exercise for valid lesson")
        void shouldReturnExerciseForValidLesson() throws Exception {
            // Given
            when(exerciseService.getExerciseForLesson(anyLong(), anyLong()))
                .thenReturn(exerciseResponse);

            // When/Then
            mockMvc.perform(get("/api/exercises/lesson/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Test Exercise"))
                .andExpect(jsonPath("$.description").value("Practice with myChessBot"))
                .andExpect(jsonPath("$.startingFen").exists())
                .andExpect(jsonPath("$.playerColor").value("white"))
                .andExpect(jsonPath("$.difficultyLevel").value("DEBUTANT"));
        }

        @Test
        @DisplayName("Should return 401 for unauthenticated request")
        void shouldReturn401ForUnauthenticated() throws Exception {
            mockMvc.perform(get("/api/exercises/lesson/1"))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "TEACHER")
        @DisplayName("Should return 403 for non-student role")
        void shouldReturn403ForNonStudent() throws Exception {
            mockMvc.perform(get("/api/exercises/lesson/1"))
                .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "STUDENT")
        @DisplayName("Should return 500 when service throws exception")
        void shouldReturn500WhenServiceThrows() throws Exception {
            // Given
            when(exerciseService.getExerciseForLesson(anyLong(), anyLong()))
                .thenThrow(new RuntimeException("Premium subscription required"));

            // When/Then
            mockMvc.perform(get("/api/exercises/lesson/1"))
                .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("GET /api/exercises/{exerciseId}")
    class GetExerciseByIdTests {

        @Test
        @WithMockUser(roles = "STUDENT")
        @DisplayName("Should return exercise by ID")
        void shouldReturnExerciseById() throws Exception {
            // Given
            when(exerciseService.getExerciseById(anyLong(), anyLong()))
                .thenReturn(exerciseResponse);

            // When/Then
            mockMvc.perform(get("/api/exercises/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Test Exercise"));
        }

        @Test
        @WithMockUser(roles = "STUDENT")
        @DisplayName("Should return 404 when exercise not found")
        void shouldReturn404WhenNotFound() throws Exception {
            // Given
            when(exerciseService.getExerciseById(anyLong(), anyLong()))
                .thenThrow(new RuntimeException("Exercise not found"));

            // When/Then
            mockMvc.perform(get("/api/exercises/999"))
                .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("GET /api/exercises")
    class GetAllExercisesTests {

        @Test
        @WithMockUser(roles = "STUDENT")
        @DisplayName("Should return all exercises for user")
        void shouldReturnAllExercises() throws Exception {
            // Given
            ExerciseResponse exercise2 = ExerciseResponse.builder()
                .id(2L)
                .lessonId(2L)
                .title("Second Exercise")
                .description("Another exercise")
                .startingFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
                .playerColor("black")
                .difficultyLevel("FACILE")
                .chessLevel("CAVALIER")
                .build();

            when(exerciseService.getAllExercisesForUser(anyLong()))
                .thenReturn(List.of(exerciseResponse, exercise2));

            // When/Then
            mockMvc.perform(get("/api/exercises"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2));
        }

        @Test
        @WithMockUser(roles = "STUDENT")
        @DisplayName("Should return empty array when no exercises")
        void shouldReturnEmptyArray() throws Exception {
            // Given
            when(exerciseService.getAllExercisesForUser(anyLong()))
                .thenReturn(List.of());

            // When/Then
            mockMvc.perform(get("/api/exercises"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Nested
    @DisplayName("Response Format Tests")
    class ResponseFormatTests {

        @Test
        @WithMockUser(roles = "STUDENT")
        @DisplayName("Should return correct FEN format")
        void shouldReturnCorrectFenFormat() throws Exception {
            // Given
            when(exerciseService.getExerciseForLesson(anyLong(), anyLong()))
                .thenReturn(exerciseResponse);

            // When/Then
            mockMvc.perform(get("/api/exercises/lesson/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startingFen").value("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"));
        }

        @Test
        @WithMockUser(roles = "STUDENT")
        @DisplayName("Should return valid player color")
        void shouldReturnValidPlayerColor() throws Exception {
            // Given
            when(exerciseService.getExerciseForLesson(anyLong(), anyLong()))
                .thenReturn(exerciseResponse);

            // When/Then
            mockMvc.perform(get("/api/exercises/lesson/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playerColor").value("white"));
        }

        @Test
        @WithMockUser(roles = "STUDENT")
        @DisplayName("Description should contain myChessBot, not l'IA")
        void descriptionShouldContainMyChessBot() throws Exception {
            // Given
            when(exerciseService.getExerciseForLesson(anyLong(), anyLong()))
                .thenReturn(exerciseResponse);

            // When/Then
            mockMvc.perform(get("/api/exercises/lesson/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Practice with myChessBot"));
        }
    }
}
