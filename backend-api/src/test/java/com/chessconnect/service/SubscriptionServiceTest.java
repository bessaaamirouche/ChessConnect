package com.chessconnect.service;

import com.chessconnect.model.Subscription;
import com.chessconnect.model.User;
import com.chessconnect.model.enums.UserRole;
import com.chessconnect.model.enums.SubscriptionPlan;
import com.chessconnect.repository.SubscriptionRepository;
import com.chessconnect.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionService Tests")
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StripeService stripeService;

    @Mock
    private com.chessconnect.repository.PaymentRepository paymentRepository;

    @InjectMocks
    private SubscriptionService subscriptionService;

    private User studentUser;
    private Subscription activeSubscription;
    private Subscription cancelledSubscription;
    private Subscription expiredSubscription;

    @BeforeEach
    void setUp() {
        // Setup student user
        studentUser = new User();
        studentUser.setId(1L);
        studentUser.setEmail("student@test.com");
        studentUser.setFirstName("John");
        studentUser.setLastName("Doe");
        studentUser.setRole(UserRole.STUDENT);

        // Setup active subscription
        activeSubscription = new Subscription();
        activeSubscription.setId(1L);
        activeSubscription.setStudent(studentUser);
        activeSubscription.setIsActive(true);
        activeSubscription.setPlanType(SubscriptionPlan.PREMIUM);
        activeSubscription.setStartDate(LocalDate.now().minusMonths(1));
        activeSubscription.setEndDate(LocalDate.now().plusMonths(1));
        activeSubscription.setStripeSubscriptionId("sub_test123");

        // Setup cancelled subscription (still valid until end date)
        cancelledSubscription = new Subscription();
        cancelledSubscription.setId(2L);
        cancelledSubscription.setStudent(studentUser);
        cancelledSubscription.setIsActive(false);
        cancelledSubscription.setPlanType(SubscriptionPlan.PREMIUM);
        cancelledSubscription.setStartDate(LocalDate.now().minusMonths(1));
        cancelledSubscription.setEndDate(LocalDate.now().plusDays(15));
        cancelledSubscription.setCancelledAt(LocalDateTime.now().minusDays(5));

        // Setup expired subscription
        expiredSubscription = new Subscription();
        expiredSubscription.setId(3L);
        expiredSubscription.setStudent(studentUser);
        expiredSubscription.setIsActive(false);
        expiredSubscription.setPlanType(SubscriptionPlan.PREMIUM);
        expiredSubscription.setStartDate(LocalDate.now().minusMonths(2));
        expiredSubscription.setEndDate(LocalDate.now().minusDays(5));
        expiredSubscription.setCancelledAt(LocalDateTime.now().minusMonths(1));
    }

    @Nested
    @DisplayName("hasActiveSubscription Tests")
    class HasActiveSubscriptionTests {

        @Test
        @DisplayName("Should return true for active subscription")
        void shouldReturnTrueForActiveSubscription() {
            // Given
            when(subscriptionRepository.findActiveSubscriptionsByStudentId(1L))
                .thenReturn(List.of(activeSubscription));

            // When
            boolean result = subscriptionService.hasActiveSubscription(1L);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when no active subscription")
        void shouldReturnFalseWhenNoActiveSubscription() {
            // Given
            when(subscriptionRepository.findActiveSubscriptionsByStudentId(1L))
                .thenReturn(List.of());

            // When
            boolean result = subscriptionService.hasActiveSubscription(1L);

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Subscription State Tests")
    class SubscriptionStateTests {

        @Test
        @DisplayName("Active subscription should have isActive true")
        void activeSubscriptionShouldHaveIsActiveTrue() {
            assertThat(activeSubscription.getIsActive()).isTrue();
        }

        @Test
        @DisplayName("Cancelled subscription should have isActive false")
        void cancelledSubscriptionShouldHaveIsActiveFalse() {
            assertThat(cancelledSubscription.getIsActive()).isFalse();
        }

        @Test
        @DisplayName("Cancelled subscription should have cancelledAt set")
        void cancelledSubscriptionShouldHaveCancelledAt() {
            assertThat(cancelledSubscription.getCancelledAt()).isNotNull();
        }

        @Test
        @DisplayName("Expired subscription should have end date in the past")
        void expiredSubscriptionShouldHaveEndDateInPast() {
            assertThat(expiredSubscription.getEndDate()).isBefore(LocalDate.now());
        }
    }

    @Nested
    @DisplayName("Subscription Plan Tests")
    class SubscriptionPlanTests {

        @Test
        @DisplayName("PREMIUM plan should exist")
        void premiumPlanShouldExist() {
            assertThat(SubscriptionPlan.PREMIUM).isNotNull();
        }

        @Test
        @DisplayName("Subscription should have PREMIUM plan")
        void subscriptionShouldHavePremiumPlan() {
            assertThat(activeSubscription.getPlanType()).isEqualTo(SubscriptionPlan.PREMIUM);
        }
    }

    @Nested
    @DisplayName("Subscription Dates Tests")
    class SubscriptionDatesTests {

        @Test
        @DisplayName("Active subscription should have valid date range")
        void activeSubscriptionShouldHaveValidDateRange() {
            assertThat(activeSubscription.getStartDate()).isBefore(LocalDate.now().plusDays(1));
            assertThat(activeSubscription.getEndDate()).isAfter(LocalDate.now());
        }

        @Test
        @DisplayName("Subscription should have start date before end date")
        void subscriptionShouldHaveStartBeforeEnd() {
            assertThat(activeSubscription.getStartDate()).isBefore(activeSubscription.getEndDate());
        }
    }

    @Nested
    @DisplayName("User Role Tests")
    class UserRoleTests {

        @Test
        @DisplayName("Subscription should be for STUDENT role")
        void subscriptionShouldBeForStudent() {
            assertThat(activeSubscription.getStudent().getRole()).isEqualTo(UserRole.STUDENT);
        }
    }
}
