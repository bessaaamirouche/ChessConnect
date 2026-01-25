package com.chessconnect.service;

import com.chessconnect.dto.wallet.TransactionResponse;
import com.chessconnect.dto.wallet.WalletBalanceResponse;
import com.chessconnect.model.CreditTransaction;
import com.chessconnect.model.StudentWallet;
import com.chessconnect.model.User;
import com.chessconnect.model.enums.Role;
import com.chessconnect.model.enums.TransactionType;
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

    @InjectMocks
    private WalletService walletService;

    private User studentUser;
    private StudentWallet wallet;
    private CreditTransaction creditTransaction;
    private CreditTransaction debitTransaction;

    @BeforeEach
    void setUp() {
        // Setup student user
        studentUser = new User();
        studentUser.setId(1L);
        studentUser.setEmail("student@test.com");
        studentUser.setFirstName("John");
        studentUser.setLastName("Doe");
        studentUser.setRole(Role.STUDENT);

        // Setup wallet
        wallet = new StudentWallet();
        wallet.setId(1L);
        wallet.setUser(studentUser);
        wallet.setBalanceCents(5000); // 50.00 EUR

        // Setup credit transaction
        creditTransaction = new CreditTransaction();
        creditTransaction.setId(1L);
        creditTransaction.setWallet(wallet);
        creditTransaction.setType(TransactionType.CREDIT);
        creditTransaction.setAmountCents(5000);
        creditTransaction.setDescription("Rechargement du portefeuille");
        creditTransaction.setCreatedAt(LocalDateTime.now().minusDays(1));

        // Setup debit transaction
        debitTransaction = new CreditTransaction();
        debitTransaction.setId(2L);
        debitTransaction.setWallet(wallet);
        debitTransaction.setType(TransactionType.DEBIT);
        debitTransaction.setAmountCents(3000);
        debitTransaction.setDescription("Paiement cours");
        debitTransaction.setCreatedAt(LocalDateTime.now());
    }

    @Nested
    @DisplayName("getBalance Tests")
    class GetBalanceTests {

        @Test
        @DisplayName("Should return wallet balance")
        void shouldReturnWalletBalance() {
            // Given
            when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));

            // When
            WalletBalanceResponse result = walletService.getBalance(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getBalanceCents()).isEqualTo(5000);
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
            WalletBalanceResponse result = walletService.getBalance(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getBalanceCents()).isEqualTo(0);
            verify(walletRepository).save(any(StudentWallet.class));
        }
    }

    @Nested
    @DisplayName("getTransactions Tests")
    class GetTransactionsTests {

        @Test
        @DisplayName("Should return all transactions")
        void shouldReturnAllTransactions() {
            // Given
            when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));
            when(transactionRepository.findByWalletIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(debitTransaction, creditTransaction));

            // When
            List<TransactionResponse> results = walletService.getTransactions(1L);

            // Then
            assertThat(results).hasSize(2);
            assertThat(results.get(0).getType()).isEqualTo("DEBIT");
            assertThat(results.get(1).getType()).isEqualTo("CREDIT");
        }

        @Test
        @DisplayName("Should return empty list when no transactions")
        void shouldReturnEmptyList() {
            // Given
            when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));
            when(transactionRepository.findByWalletIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of());

            // When
            List<TransactionResponse> results = walletService.getTransactions(1L);

            // Then
            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("Credit Operations Tests")
    class CreditOperationsTests {

        @Test
        @DisplayName("Should credit wallet")
        void shouldCreditWallet() {
            // Given
            when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));
            when(walletRepository.save(any(StudentWallet.class))).thenAnswer(i -> i.getArgument(0));
            when(transactionRepository.save(any(CreditTransaction.class))).thenAnswer(invocation -> {
                CreditTransaction saved = invocation.getArgument(0);
                saved.setId(3L);
                return saved;
            });

            // When
            walletService.creditWallet(1L, 2000, "Test credit");

            // Then
            verify(walletRepository).save(argThat(w -> w.getBalanceCents() == 7000));
            verify(transactionRepository).save(argThat(t ->
                t.getType() == TransactionType.CREDIT && t.getAmountCents() == 2000
            ));
        }

        @Test
        @DisplayName("Should debit wallet")
        void shouldDebitWallet() {
            // Given
            when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));
            when(walletRepository.save(any(StudentWallet.class))).thenAnswer(i -> i.getArgument(0));
            when(transactionRepository.save(any(CreditTransaction.class))).thenAnswer(invocation -> {
                CreditTransaction saved = invocation.getArgument(0);
                saved.setId(3L);
                return saved;
            });

            // When
            walletService.debitWallet(1L, 2000, "Test debit");

            // Then
            verify(walletRepository).save(argThat(w -> w.getBalanceCents() == 3000));
            verify(transactionRepository).save(argThat(t ->
                t.getType() == TransactionType.DEBIT && t.getAmountCents() == 2000
            ));
        }

        @Test
        @DisplayName("Should throw exception when insufficient balance")
        void shouldThrowWhenInsufficientBalance() {
            // Given
            when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));

            // When/Then
            assertThatThrownBy(() -> walletService.debitWallet(1L, 10000, "Too much"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Insufficient");
        }
    }

    @Nested
    @DisplayName("Transaction Type Tests")
    class TransactionTypeTests {

        @Test
        @DisplayName("CREDIT type should be for adding money")
        void creditTypeShouldBeForAddingMoney() {
            assertThat(TransactionType.CREDIT).isNotNull();
            assertThat(creditTransaction.getType()).isEqualTo(TransactionType.CREDIT);
        }

        @Test
        @DisplayName("DEBIT type should be for spending money")
        void debitTypeShouldBeForSpendingMoney() {
            assertThat(TransactionType.DEBIT).isNotNull();
            assertThat(debitTransaction.getType()).isEqualTo(TransactionType.DEBIT);
        }

        @Test
        @DisplayName("REFUND type should exist for refunds")
        void refundTypeShouldExist() {
            assertThat(TransactionType.REFUND).isNotNull();
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
            WalletBalanceResponse result = walletService.getBalance(1L);

            // Then
            assertThat(result.getBalanceCents()).isEqualTo(0);
        }
    }
}
