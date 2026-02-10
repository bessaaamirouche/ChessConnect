package com.chessconnect.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("GroupPricingCalculator Tests")
class GroupPricingCalculatorTest {

    @Nested
    @DisplayName("calculateParticipantPrice")
    class CalculateParticipantPrice {

        @Test
        @DisplayName("Group of 2: each pays 60% of teacher rate")
        void groupOf2Pays60Percent() {
            // 5000 cents (50 EUR) * 60% = 3000 cents
            assertThat(GroupPricingCalculator.calculateParticipantPrice(5000, 2)).isEqualTo(3000);
        }

        @Test
        @DisplayName("Group of 3: each pays 45% of teacher rate")
        void groupOf3Pays45Percent() {
            // 5000 cents * 45% = 2250 cents
            assertThat(GroupPricingCalculator.calculateParticipantPrice(5000, 3)).isEqualTo(2250);
        }

        @Test
        @DisplayName("Should throw for invalid group size 1")
        void shouldThrowForSize1() {
            assertThatThrownBy(() -> GroupPricingCalculator.calculateParticipantPrice(5000, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Group size must be 2 or 3");
        }

        @Test
        @DisplayName("Should throw for invalid group size 4")
        void shouldThrowForSize4() {
            assertThatThrownBy(() -> GroupPricingCalculator.calculateParticipantPrice(5000, 4))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Group size must be 2 or 3");
        }

        @Test
        @DisplayName("Should handle zero rate")
        void shouldHandleZeroRate() {
            assertThat(GroupPricingCalculator.calculateParticipantPrice(0, 2)).isEqualTo(0);
            assertThat(GroupPricingCalculator.calculateParticipantPrice(0, 3)).isEqualTo(0);
        }

        @Test
        @DisplayName("Should truncate (integer division) for odd amounts")
        void shouldTruncateForOddAmounts() {
            // 333 * 60 / 100 = 199 (integer division)
            assertThat(GroupPricingCalculator.calculateParticipantPrice(333, 2)).isEqualTo(199);
            // 333 * 45 / 100 = 149 (integer division: 14985/100)
            assertThat(GroupPricingCalculator.calculateParticipantPrice(333, 3)).isEqualTo(149);
        }
    }

    @Nested
    @DisplayName("calculateTotalCollected")
    class CalculateTotalCollected {

        @Test
        @DisplayName("Total for group of 2: 2 * 60% of rate = 120% of rate")
        void totalForGroupOf2() {
            // 5000 * 60% = 3000 per person, 3000 * 2 = 6000
            assertThat(GroupPricingCalculator.calculateTotalCollected(5000, 2)).isEqualTo(6000);
        }

        @Test
        @DisplayName("Total for group of 3: 3 * 45% of rate = 135% of rate")
        void totalForGroupOf3() {
            // 5000 * 45% = 2250 per person, 2250 * 3 = 6750
            assertThat(GroupPricingCalculator.calculateTotalCollected(5000, 3)).isEqualTo(6750);
        }

        @Test
        @DisplayName("Total with zero rate")
        void totalWithZeroRate() {
            assertThat(GroupPricingCalculator.calculateTotalCollected(0, 2)).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("calculateCommission")
    class CalculateCommission {

        @Test
        @DisplayName("12.5% commission on 6000 = 750")
        void commissionOn6000() {
            // 6000 * 125 / 1000 = 750
            assertThat(GroupPricingCalculator.calculateCommission(6000)).isEqualTo(750);
        }

        @Test
        @DisplayName("12.5% commission on 6750 = 843")
        void commissionOn6750() {
            // 6750 * 125 / 1000 = 843 (integer: 843750/1000)
            assertThat(GroupPricingCalculator.calculateCommission(6750)).isEqualTo(843);
        }

        @Test
        @DisplayName("Commission on zero")
        void commissionOnZero() {
            assertThat(GroupPricingCalculator.calculateCommission(0)).isEqualTo(0);
        }

        @Test
        @DisplayName("Commission on small amount")
        void commissionOnSmallAmount() {
            // 100 * 125 / 1000 = 12
            assertThat(GroupPricingCalculator.calculateCommission(100)).isEqualTo(12);
        }
    }

    @Nested
    @DisplayName("calculateTeacherEarnings")
    class CalculateTeacherEarnings {

        @Test
        @DisplayName("Teacher earnings = amount - commission")
        void earningsEqualsAmountMinusCommission() {
            // 6000 - 750 = 5250
            assertThat(GroupPricingCalculator.calculateTeacherEarnings(6000)).isEqualTo(5250);
        }

        @Test
        @DisplayName("Teacher earnings for group of 3 total")
        void earningsForGroupOf3() {
            // 6750 - 843 = 5907
            assertThat(GroupPricingCalculator.calculateTeacherEarnings(6750)).isEqualTo(5907);
        }

        @Test
        @DisplayName("Teacher earnings with zero")
        void earningsWithZero() {
            assertThat(GroupPricingCalculator.calculateTeacherEarnings(0)).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("savingsPercent")
    class SavingsPercent {

        @Test
        @DisplayName("40% savings for group of 2")
        void savingsForGroupOf2() {
            assertThat(GroupPricingCalculator.savingsPercent(2)).isEqualTo(40);
        }

        @Test
        @DisplayName("55% savings for group of 3")
        void savingsForGroupOf3() {
            assertThat(GroupPricingCalculator.savingsPercent(3)).isEqualTo(55);
        }

        @Test
        @DisplayName("0% savings for invalid size")
        void savingsForInvalidSize() {
            assertThat(GroupPricingCalculator.savingsPercent(1)).isEqualTo(0);
            assertThat(GroupPricingCalculator.savingsPercent(4)).isEqualTo(0);
        }
    }
}
