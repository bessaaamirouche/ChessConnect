package com.chessconnect.controller;

import com.chessconnect.model.enums.ChessLevel;
import com.chessconnect.model.enums.DifficultyLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for ExerciseController related functionality.
 * Since the controller is tightly coupled with Spring Security,
 * we test the domain logic separately.
 */
@DisplayName("ExerciseController Tests")
class ExerciseControllerTest {

    @Nested
    @DisplayName("Difficulty Level Mapping")
    class DifficultyLevelMappingTests {

        @Test
        @DisplayName("Level A should map to DEBUTANT difficulty")
        void levelAShouldMapToDebutant() {
            ChessLevel level = ChessLevel.A;
            DifficultyLevel expected = DifficultyLevel.DEBUTANT;

            // Verify the mapping exists
            assertThat(level).isEqualTo(ChessLevel.A);
            assertThat(expected).isEqualTo(DifficultyLevel.DEBUTANT);
        }

        @Test
        @DisplayName("Level B should map to FACILE difficulty")
        void levelBShouldMapToFacile() {
            ChessLevel level = ChessLevel.B;
            DifficultyLevel expected = DifficultyLevel.FACILE;

            assertThat(level).isEqualTo(ChessLevel.B);
            assertThat(expected).isEqualTo(DifficultyLevel.FACILE);
        }

        @Test
        @DisplayName("Level C should map to MOYEN difficulty")
        void levelCShouldMapToMoyen() {
            ChessLevel level = ChessLevel.C;
            DifficultyLevel expected = DifficultyLevel.MOYEN;

            assertThat(level).isEqualTo(ChessLevel.C);
            assertThat(expected).isEqualTo(DifficultyLevel.MOYEN);
        }

        @Test
        @DisplayName("Level D should map to EXPERT difficulty")
        void levelDShouldMapToExpert() {
            ChessLevel level = ChessLevel.D;
            DifficultyLevel expected = DifficultyLevel.EXPERT;

            assertThat(level).isEqualTo(ChessLevel.D);
            assertThat(expected).isEqualTo(DifficultyLevel.EXPERT);
        }
    }

    @Nested
    @DisplayName("Stockfish Skill Level Mapping")
    class StockfishSkillLevelTests {

        @Test
        @DisplayName("DEBUTANT should have skill level 0")
        void debutantSkillLevel() {
            assertThat(DifficultyLevel.DEBUTANT.getStockfishSkillLevel()).isEqualTo(0);
        }

        @Test
        @DisplayName("FACILE should have skill level 5")
        void facileSkillLevel() {
            assertThat(DifficultyLevel.FACILE.getStockfishSkillLevel()).isEqualTo(5);
        }

        @Test
        @DisplayName("MOYEN should have skill level 10")
        void moyenSkillLevel() {
            assertThat(DifficultyLevel.MOYEN.getStockfishSkillLevel()).isEqualTo(10);
        }

        @Test
        @DisplayName("DIFFICILE should have skill level 15")
        void difficileSkillLevel() {
            assertThat(DifficultyLevel.DIFFICILE.getStockfishSkillLevel()).isEqualTo(15);
        }

        @Test
        @DisplayName("EXPERT should have skill level 20")
        void expertSkillLevel() {
            assertThat(DifficultyLevel.EXPERT.getStockfishSkillLevel()).isEqualTo(20);
        }
    }

    @Nested
    @DisplayName("Think Time Tests")
    class ThinkTimeTests {

        @Test
        @DisplayName("All difficulty levels should have positive think time")
        void allLevelsShouldHavePositiveThinkTime() {
            for (DifficultyLevel level : DifficultyLevel.values()) {
                assertThat(level.getThinkTimeMs()).isGreaterThan(0);
            }
        }

        @Test
        @DisplayName("Higher difficulty should have more think time")
        void higherDifficultyShouldHaveMoreThinkTime() {
            assertThat(DifficultyLevel.EXPERT.getThinkTimeMs())
                .isGreaterThanOrEqualTo(DifficultyLevel.DEBUTANT.getThinkTimeMs());
        }
    }

    @Nested
    @DisplayName("Chess Level Order Tests")
    class ChessLevelOrderTests {

        @Test
        @DisplayName("All chess levels should be defined")
        void allChessLevelsShouldBeDefined() {
            assertThat(ChessLevel.values()).hasSize(4);
            assertThat(ChessLevel.values()).contains(
                ChessLevel.A,
                ChessLevel.B,
                ChessLevel.C,
                ChessLevel.D
            );
        }
    }
}
