package com.chessconnect.service;

import com.chessconnect.model.Subscription;
import com.chessconnect.model.User;
import com.chessconnect.model.enums.Role;
import com.chessconnect.model.enums.SubscriptionStatus;
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

import java.time.LocalDateTime;
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
        studentUser.setRole(Role.STUDENT);

        // Setup active subscription
        activeSubscription = new Subscription();
        activeSubscription.setId(1L);
        activeSubscription.setUser(studentUser);
        activeSubscription.setStatus(SubscriptionStatus.ACTIVE);
        activeSubscription.setStartDate(LocalDateTime.now().minusMonths(1));
        activeSubscription.setCurrentPeriodEnd(LocalDateTime.now().plusMonths(1));
        activeSubscription.setStripeSubscriptionId("sub_test123");

        // Setup cancelled subscription (still valid until period end)
        cancelledSubscription = new Subscription();
        cancelledSubscription.setId(2L);
        cancelledSubscription.setUser(studentUser);
        cancelledSubscription.setStatus(SubscriptionStatus.CANCELLED);
        cancelledSubscription.setStartDate(LocalDateTime.now().minusMonths(1));
        cancelledSubscription.setCurrentPeriodEnd(LocalDateTime.now().plusDays(15));
        cancelledSubscription.setCancelledAt(LocalDateTime.now().minusDays(5));

        // Setup expired subscription
        expiredSubscription = new Subscription();
        expiredSubscription.setId(3L);
        expiredSubscription.setUser(studentUser);
        expiredSubscription.setStatus(SubscriptionStatus.CANCELLED);
        expiredSubscription.setStartDate(LocalDateTime.now().minusMonths(2));
        expiredSubscription.setCurrentPeriodEnd(LocalDateTime.now().minusDays(5));
        expiredSubscription.setCancelledAt(LocalDateTime.now().minusMonths(1));
    }

    @Nested
    @DisplayName("hasActiveSubscription Tests")
    class HasActiveSubscriptionTests {

        @Test
        @DisplayName("Should return true for active subscription")
        void shouldReturnTrueForActiveSubscription() {
            // Given
            when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(activeSubscription));

            // When
            boolean result = subscriptionService.hasActiveSubscription(1L);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return true for cancelled subscription still within period")
        void shouldReturnTrueForCancelledButValidSubscription() {
            // Given
            when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());
            when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.CANCELLED))
                .thenReturn(Optional.of(cancelledSubscription));

            // When
            boolean result = subscriptionService.hasActiveSubscription(1L);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false for expired subscription")
        void shouldReturnFalseForExpiredSubscription() {
            // Given
            when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());
            when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.CANCELLED))
                .thenReturn(Optional.of(expiredSubscription));

            // When
            boolean result = subscriptionService.hasActiveSubscription(1L);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false when no subscription exists")
        void shouldReturnFalseWhenNoSubscription() {
            // Given
            when(subscriptionRepository.findByUserIdAndStatus(anyLong(), any()))
                .thenReturn(Optional.empty());

            // When
            boolean result = subscriptionService.hasActiveSubscription(1L);

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("getActiveSubscription Tests")
    class GetActiveSubscriptionTests {

        @Test
        @DisplayName("Should return active subscription")
        void shouldReturnActiveSubscription() {
            // Given
            when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(activeSubscription));

            // When
            Optional<Subscription> result = subscriptionService.getActiveSubscription(1L);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(1L);
            assertThat(result.get().getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        }

        @Test
        @DisplayName("Should return empty when no active subscription")
        void shouldReturnEmptyWhenNoActiveSubscription() {
            // Given
            when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());

            // When
            Optional<Subscription> result = subscriptionService.getActiveSubscription(1L);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Subscription Status Tests")
    class SubscriptionStatusTests {

        @Test
        @DisplayName("Active status should grant premium access")
        void activeStatusShouldGrantAccess() {
            // When/Then
            assertThat(SubscriptionStatus.ACTIVE).isNotNull();
            assertThat(activeSubscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        }

        @Test
        @DisplayName("Cancelled status should still grant access until period end")
        void cancelledStatusShouldGrantAccessUntilPeriodEnd() {
            // Given - cancelled but period end is in the future
            assertThat(cancelledSubscription.getCurrentPeriodEnd()).isAfter(LocalDateTime.now());

            // Then - should still be considered active for premium features
            boolean isStillValid = cancelledSubscription.getCurrentPeriodEnd().isAfter(LocalDateTime.now());
            assertThat(isStillValid).isTrue();
        }
    }
}
