package com.chessconnect.service;

import com.chessconnect.dto.wallet.CreditTransactionResponse;
import com.chessconnect.dto.wallet.WalletResponse;
import com.chessconnect.model.CreditTransaction;
import com.chessconnect.model.StudentWallet;
import com.chessconnect.model.User;
import com.chessconnect.model.enums.UserRole;
import com.chessconnect.model.enums.CreditTransactionType;
import com.chessconnect.repository.CreditTransactionRepository;
import com.chessconnect.repository.StudentWalletRepository;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WalletService Tests")
class WalletServiceTest {

    @Mock
    private StudentWalletRepository walletRepository;

    @Mock
    private CreditTransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StripeService stripeService;

    @InjectMocks
    private WalletService walletService;

    private User studentUser;
    private StudentWallet wallet;
    private CreditTransaction topUpTransaction;
    private CreditTransaction lessonPaymentTransaction;

    @BeforeEach
    void setUp() {
        // Setup student user
        studentUser = new User();
        studentUser.setId(1L);
        studentUser.setEmail("student@test.com");
        studentUser.setFirstName("John");
        studentUser.setLastName("Doe");
        studentUser.setRole(UserRole.STUDENT);

        // Setup wallet
        wallet = new StudentWallet();
        wallet.setId(1L);
        wallet.setUser(studentUser);
        wallet.setBalanceCents(5000); // 50.00 EUR

        // Setup top-up transaction
        topUpTransaction = new CreditTransaction();
        topUpTransaction.setId(1L);
        topUpTransaction.setUser(studentUser);
        topUpTransaction.setTransactionType(CreditTransactionType.TOPUP);
        topUpTransaction.setAmountCents(5000);
        topUpTransaction.setDescription("Recharge du portefeuille");
        topUpTransaction.setCreatedAt(LocalDateTime.now().minusDays(1));

        // Setup lesson payment transaction
        lessonPaymentTransaction = new CreditTransaction();
        lessonPaymentTransaction.setId(2L);
        lessonPaymentTransaction.setUser(studentUser);
        lessonPaymentTransaction.setTransactionType(CreditTransactionType.LESSON_PAYMENT);
        lessonPaymentTransaction.setAmountCents(3000);
        lessonPaymentTransaction.setDescription("Paiement cours");
        lessonPaymentTransaction.setCreatedAt(LocalDateTime.now());
    }

    @Nested
    @DisplayName("getWallet Tests")
    class GetWalletTests {

        @Test
        @DisplayName("Should return wallet info")
        void shouldReturnWalletInfo() {
            // Given
            when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));

            // When
            WalletResponse result = walletService.getWallet(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getBalanceCents()).isEqualTo(5000);
        }

        @Test
        @DisplayName("Should return empty wallet if not exists")
        void shouldReturnEmptyWalletIfNotExists() {
            // Given
            when(walletRepository.findByUserId(1L)).thenReturn(Optional.empty());

            // When
            WalletResponse result = walletService.getWallet(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getBalanceCents()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("getBalance Tests")
    class GetBalanceTests {

        @Test
        @DisplayName("Should return balance")
        void shouldReturnBalance() {
            // Given
            when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));

            // When
            int result = walletService.getBalance(1L);

            // Then
            assertThat(result).isEqualTo(5000);
        }

        @Test
        @DisplayName("Should return 0 if wallet not exists")
        void shouldReturnZeroIfNoWallet() {
            // Given
            when(walletRepository.findByUserId(1L)).thenReturn(Optional.empty());

            // When
            int result = walletService.getBalance(1L);

            // Then
            assertThat(result).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("getTransactions Tests")
    class GetTransactionsTests {

        @Test
        @DisplayName("Should return all transactions")
        void shouldReturnAllTransactions() {
            // Given
            when(transactionRepository.findByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(lessonPaymentTransaction, topUpTransaction));

            // When
            List<CreditTransactionResponse> results = walletService.getTransactions(1L);

            // Then
            assertThat(results).hasSize(2);
        }

        @Test
        @DisplayName("Should return empty list when no transactions")
        void shouldReturnEmptyList() {
            // Given
            when(transactionRepository.findByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of());

            // When
            List<CreditTransactionResponse> results = walletService.getTransactions(1L);

            // Then
            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("hasEnoughCredit Tests")
    class HasEnoughCreditTests {

        @Test
        @DisplayName("Should return true when balance is sufficient")
        void shouldReturnTrueWhenSufficient() {
            // Given
            when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));

            // When
            boolean result = walletService.hasEnoughCredit(1L, 3000);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when balance is insufficient")
        void shouldReturnFalseWhenInsufficient() {
            // Given
            when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));

            // When
            boolean result = walletService.hasEnoughCredit(1L, 10000);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false when wallet not exists")
        void shouldReturnFalseWhenNoWallet() {
            // Given
            when(walletRepository.findByUserId(1L)).thenReturn(Optional.empty());

            // When
            boolean result = walletService.hasEnoughCredit(1L, 100);

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("getOrCreateWallet Tests")
    class GetOrCreateWalletTests {

        @Test
        @DisplayName("Should return existing wallet")
        void shouldReturnExistingWallet() {
            // Given
            when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));

            // When
            StudentWallet result = walletService.getOrCreateWallet(1L);

            // Then
            assertThat(result).isEqualTo(wallet);
            verify(walletRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should create wallet if not exists")
        void shouldCreateWalletIfNotExists() {
            // Given
            when(walletRepository.findByUserId(1L)).thenReturn(Optional.empty());
            when(userRepository.findById(1L)).thenReturn(Optional.of(studentUser));
            when(walletRepository.save(any(StudentWallet.class))).thenAnswer(invocation -> {
                StudentWallet saved = invocation.getArgument(0);
                saved.setId(1L);
                return saved;
            });

            // When
            StudentWallet result = walletService.getOrCreateWallet(1L);

            // Then
            assertThat(result).isNotNull();
            verify(walletRepository).save(any(StudentWallet.class));
        }

        @Test
        @DisplayName("Should throw exception for non-student users")
        void shouldThrowExceptionForNonStudent() {
            // Given
            User teacherUser = new User();
            teacherUser.setId(2L);
            teacherUser.setRole(UserRole.TEACHER);

            when(walletRepository.findByUserId(2L)).thenReturn(Optional.empty());
            when(userRepository.findById(2L)).thenReturn(Optional.of(teacherUser));

            // When/Then
            assertThatThrownBy(() -> walletService.getOrCreateWallet(2L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only students");
        }
    }

    @Nested
    @DisplayName("Transaction Type Tests")
    class TransactionTypeTests {

        @Test
        @DisplayName("TOPUP type should be for adding money")
        void topupTypeShouldBeForAddingMoney() {
            assertThat(CreditTransactionType.TOPUP).isNotNull();
            assertThat(topUpTransaction.getTransactionType()).isEqualTo(CreditTransactionType.TOPUP);
        }

        @Test
        @DisplayName("LESSON_PAYMENT type should be for spending money")
        void lessonPaymentTypeShouldBeForSpendingMoney() {
            assertThat(CreditTransactionType.LESSON_PAYMENT).isNotNull();
            assertThat(lessonPaymentTransaction.getTransactionType()).isEqualTo(CreditTransactionType.LESSON_PAYMENT);
        }

        @Test
        @DisplayName("REFUND type should exist for refunds")
        void refundTypeShouldExist() {
            assertThat(CreditTransactionType.REFUND).isNotNull();
        }
    }

    @Nested
    @DisplayName("Balance Calculation Tests")
    class BalanceCalculationTests {

        @Test
        @DisplayName("Balance should be in cents")
        void balanceShouldBeInCents() {
            // 50.00 EUR = 5000 cents
            assertThat(wallet.getBalanceCents()).isEqualTo(5000);
        }

        @Test
        @DisplayName("Should handle zero balance")
        void shouldHandleZeroBalance() {
            // Given
            wallet.setBalanceCents(0);
            when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));

            // When
            int result = walletService.getBalance(1L);

            // Then
            assertThat(result).isEqualTo(0);
        }
    }
}
