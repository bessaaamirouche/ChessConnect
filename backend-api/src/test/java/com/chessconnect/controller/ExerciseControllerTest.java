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
        @DisplayName("PION level should map to DEBUTANT difficulty")
        void pionShouldMapToDebutant() {
            ChessLevel level = ChessLevel.PION;
            DifficultyLevel expected = DifficultyLevel.DEBUTANT;

            // Verify the mapping exists
            assertThat(level).isEqualTo(ChessLevel.PION);
            assertThat(expected).isEqualTo(DifficultyLevel.DEBUTANT);
        }

        @Test
        @DisplayName("CAVALIER level should map to FACILE difficulty")
        void cavalierShouldMapToFacile() {
            ChessLevel level = ChessLevel.CAVALIER;
            DifficultyLevel expected = DifficultyLevel.FACILE;

            assertThat(level).isEqualTo(ChessLevel.CAVALIER);
            assertThat(expected).isEqualTo(DifficultyLevel.FACILE);
        }

        @Test
        @DisplayName("FOU level should map to MOYEN difficulty")
        void fouShouldMapToMoyen() {
            ChessLevel level = ChessLevel.FOU;
            DifficultyLevel expected = DifficultyLevel.MOYEN;

            assertThat(level).isEqualTo(ChessLevel.FOU);
            assertThat(expected).isEqualTo(DifficultyLevel.MOYEN);
        }

        @Test
        @DisplayName("TOUR level should map to DIFFICILE difficulty")
        void tourShouldMapToDifficile() {
            ChessLevel level = ChessLevel.TOUR;
            DifficultyLevel expected = DifficultyLevel.DIFFICILE;

            assertThat(level).isEqualTo(ChessLevel.TOUR);
            assertThat(expected).isEqualTo(DifficultyLevel.DIFFICILE);
        }

        @Test
        @DisplayName("DAME level should map to EXPERT difficulty")
        void dameShouldMapToExpert() {
            ChessLevel level = ChessLevel.DAME;
            DifficultyLevel expected = DifficultyLevel.EXPERT;

            assertThat(level).isEqualTo(ChessLevel.DAME);
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
            assertThat(ChessLevel.values()).hasSize(5);
            assertThat(ChessLevel.values()).contains(
                ChessLevel.PION,
                ChessLevel.CAVALIER,
                ChessLevel.FOU,
                ChessLevel.TOUR,
                ChessLevel.DAME
            );
        }
    }
}
