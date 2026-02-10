package com.chessconnect.service;

import com.chessconnect.dto.group.BookGroupLessonRequest;
import com.chessconnect.dto.group.GroupInvitationResponse;
import com.chessconnect.dto.group.GroupLessonResponse;
import com.chessconnect.model.*;
import com.chessconnect.model.enums.LessonStatus;
import com.chessconnect.model.enums.PaymentStatus;
import com.chessconnect.model.enums.PaymentType;
import com.chessconnect.model.enums.UserRole;
import com.chessconnect.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GroupLessonService Tests")
class GroupLessonServiceTest {

    @Mock private LessonRepository lessonRepository;
    @Mock private LessonParticipantRepository participantRepository;
    @Mock private GroupInvitationRepository invitationRepository;
    @Mock private UserRepository userRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private WalletService walletService;
    @Mock private InvoiceService invoiceService;
    @Mock private TeacherBalanceService teacherBalanceService;
    @Mock private ProgressRepository progressRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private GroupLessonService groupLessonService;

    private User student;
    private User teacher;
    private User student2;
    private Lesson groupLesson;
    private GroupInvitation invitation;

    @BeforeEach
    void setUp() {
        student = new User();
        student.setId(1L);
        student.setFirstName("Denis");
        student.setLastName("Benoit");
        student.setRole(UserRole.STUDENT);

        teacher = new User();
        teacher.setId(10L);
        teacher.setFirstName("Samuel");
        teacher.setLastName("Benis");
        teacher.setRole(UserRole.TEACHER);
        teacher.setHourlyRateCents(5000);

        student2 = new User();
        student2.setId(2L);
        student2.setFirstName("Jean");
        student2.setLastName("Dupont");
        student2.setRole(UserRole.STUDENT);

        groupLesson = new Lesson();
        groupLesson.setId(100L);
        groupLesson.setStudent(student);
        groupLesson.setTeacher(teacher);
        groupLesson.setScheduledAt(LocalDateTime.now().plusDays(3));
        groupLesson.setDurationMinutes(60);
        groupLesson.setPriceCents(5000);
        groupLesson.setIsGroupLesson(true);
        groupLesson.setMaxParticipants(2);
        groupLesson.setGroupStatus("OPEN");
        groupLesson.setStatus(LessonStatus.PENDING);
        groupLesson.setIsFromSubscription(false);
        groupLesson.setEarningsCredited(false);

        invitation = new GroupInvitation();
        invitation.setId(50L);
        invitation.setToken("test-token-uuid");
        invitation.setLesson(groupLesson);
        invitation.setCreatedBy(student);
        invitation.setMaxParticipants(2);
        invitation.setExpiresAt(LocalDateTime.now().plusDays(2));
    }

    // ─── CREATE GROUP LESSON ──────────────────────────────────

    @Nested
    @DisplayName("createGroupLesson")
    class CreateGroupLesson {

        @Test
        @DisplayName("Should create group lesson with OPEN status and CREATOR participant")
        void shouldCreateGroupLesson() {
            // Given
            BookGroupLessonRequest request = new BookGroupLessonRequest(
                    10L, LocalDateTime.now().plusDays(3), 60, "notes", 2, null);

            when(userRepository.findById(1L)).thenReturn(Optional.of(student));
            when(userRepository.findById(10L)).thenReturn(Optional.of(teacher));
            when(lessonRepository.findTeacherLessonsBetween(anyLong(), any(), any())).thenReturn(Collections.emptyList());
            when(lessonRepository.findStudentLessonsBetween(anyLong(), any(), any())).thenReturn(Collections.emptyList());
            when(lessonRepository.save(any(Lesson.class))).thenAnswer(inv -> {
                Lesson l = inv.getArgument(0);
                l.setId(100L);
                l.setParticipants(new ArrayList<>());
                return l;
            });
            when(participantRepository.save(any(LessonParticipant.class))).thenAnswer(inv -> inv.getArgument(0));
            when(invitationRepository.save(any(GroupInvitation.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            GroupLessonResponse response = groupLessonService.createGroupLesson(1L, request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.isGroupLesson()).isTrue();
            assertThat(response.maxParticipants()).isEqualTo(2);

            // Verify participant was created as CREATOR
            ArgumentCaptor<LessonParticipant> participantCaptor = ArgumentCaptor.forClass(LessonParticipant.class);
            verify(participantRepository).save(participantCaptor.capture());
            LessonParticipant saved = participantCaptor.getValue();
            assertThat(saved.getRole()).isEqualTo("CREATOR");
            assertThat(saved.getStatus()).isEqualTo("ACTIVE");
            assertThat(saved.getPricePaidCents()).isEqualTo(3000); // 60% of 5000

            // Verify invitation was created
            verify(invitationRepository).save(any(GroupInvitation.class));
        }

        @Test
        @DisplayName("Should reject invalid group size")
        void shouldRejectInvalidGroupSize() {
            BookGroupLessonRequest request = new BookGroupLessonRequest(
                    10L, LocalDateTime.now().plusDays(3), 60, null, 5, null);
            when(userRepository.findById(1L)).thenReturn(Optional.of(student));

            assertThatThrownBy(() -> groupLessonService.createGroupLesson(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Group size must be 2 or 3");
        }

        @Test
        @DisplayName("Should reject if teacher not found")
        void shouldRejectTeacherNotFound() {
            BookGroupLessonRequest request = new BookGroupLessonRequest(
                    99L, LocalDateTime.now().plusDays(3), 60, null, 2, null);
            when(userRepository.findById(1L)).thenReturn(Optional.of(student));
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> groupLessonService.createGroupLesson(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Teacher not found");
        }

        @Test
        @DisplayName("Should reject if user is not a student")
        void shouldRejectNonStudent() {
            BookGroupLessonRequest request = new BookGroupLessonRequest(
                    10L, LocalDateTime.now().plusDays(3), 60, null, 2, null);
            when(userRepository.findById(10L)).thenReturn(Optional.of(teacher));

            assertThatThrownBy(() -> groupLessonService.createGroupLesson(10L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only students can create group lessons");
        }
    }

    // ─── GET INVITATION DETAILS ───────────────────────────────

    @Nested
    @DisplayName("getInvitationDetails")
    class GetInvitationDetails {

        @Test
        @DisplayName("Should return invitation details with price")
        void shouldReturnInvitationDetails() {
            // Given
            groupLesson.setParticipants(new ArrayList<>());
            when(invitationRepository.findByToken("test-token-uuid")).thenReturn(Optional.of(invitation));

            // When
            GroupInvitationResponse response = groupLessonService.getInvitationDetails("test-token-uuid");

            // Then
            assertThat(response).isNotNull();
            assertThat(response.token()).isEqualTo("test-token-uuid");
            assertThat(response.pricePerPersonCents()).isEqualTo(3000); // 60% of 5000
            assertThat(response.targetGroupSize()).isEqualTo(2);
            assertThat(response.teacherFirstName()).isEqualTo("Samuel");
        }

        @Test
        @DisplayName("Should throw for invalid token")
        void shouldThrowForInvalidToken() {
            when(invitationRepository.findByToken("bad-token")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> groupLessonService.getInvitationDetails("bad-token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invitation not found");
        }
    }

    // ─── JOIN WITH CREDIT ─────────────────────────────────────

    @Nested
    @DisplayName("joinWithCredit")
    class JoinWithCredit {

        @Test
        @DisplayName("Should add participant and deduct wallet")
        void shouldJoinAndDeductWallet() {
            // Given
            groupLesson.setParticipants(new ArrayList<>());
            when(invitationRepository.findByToken("test-token-uuid")).thenReturn(Optional.of(invitation));
            when(lessonRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(groupLesson));
            when(participantRepository.existsByLessonIdAndStudentId(100L, 2L)).thenReturn(false);
            when(userRepository.findById(2L)).thenReturn(Optional.of(student2));
            when(participantRepository.save(any(LessonParticipant.class))).thenAnswer(inv -> inv.getArgument(0));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
            when(participantRepository.countByLessonIdAndStatus(100L, "ACTIVE")).thenReturn(2);
            when(lessonRepository.findStudentLessonsBetween(anyLong(), any(), any())).thenReturn(Collections.emptyList());

            // When
            GroupLessonResponse response = groupLessonService.joinWithCredit(2L, "test-token-uuid");

            // Then
            assertThat(response).isNotNull();
            verify(walletService).checkAndDeductCredit(2L, 3000);
            verify(walletService).linkDeductionToLesson(eq(2L), eq(groupLesson), eq(3000));
            verify(invoiceService).generateInvoicesForCreditPayment(2L, 10L, 100L, 3000);

            ArgumentCaptor<LessonParticipant> captor = ArgumentCaptor.forClass(LessonParticipant.class);
            verify(participantRepository).save(captor.capture());
            assertThat(captor.getValue().getRole()).isEqualTo("PARTICIPANT");
            assertThat(captor.getValue().getPricePaidCents()).isEqualTo(3000);
        }

        @Test
        @DisplayName("Should set FULL status when group is complete")
        void shouldSetFullWhenComplete() {
            // Given
            groupLesson.setParticipants(new ArrayList<>());
            when(invitationRepository.findByToken("test-token-uuid")).thenReturn(Optional.of(invitation));
            when(lessonRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(groupLesson));
            when(participantRepository.existsByLessonIdAndStudentId(100L, 2L)).thenReturn(false);
            when(userRepository.findById(2L)).thenReturn(Optional.of(student2));
            when(participantRepository.save(any(LessonParticipant.class))).thenAnswer(inv -> inv.getArgument(0));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
            when(participantRepository.countByLessonIdAndStatus(100L, "ACTIVE")).thenReturn(2); // Now full (max=2)
            when(lessonRepository.findStudentLessonsBetween(anyLong(), any(), any())).thenReturn(Collections.emptyList());

            // When
            groupLessonService.joinWithCredit(2L, "test-token-uuid");

            // Then
            assertThat(groupLesson.getGroupStatus()).isEqualTo("FULL");
            verify(lessonRepository).save(groupLesson);
        }

        @Test
        @DisplayName("Should reject if already a participant")
        void shouldRejectAlreadyParticipant() {
            when(invitationRepository.findByToken("test-token-uuid")).thenReturn(Optional.of(invitation));
            when(lessonRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(groupLesson));
            when(participantRepository.existsByLessonIdAndStudentId(100L, 2L)).thenReturn(true);

            assertThatThrownBy(() -> groupLessonService.joinWithCredit(2L, "test-token-uuid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already a participant");
        }

        @Test
        @DisplayName("Should reject if group is full")
        void shouldRejectIfFull() {
            // Make the lesson appear full
            LessonParticipant p1 = new LessonParticipant();
            p1.setStatus("ACTIVE");
            LessonParticipant p2 = new LessonParticipant();
            p2.setStatus("ACTIVE");
            groupLesson.setParticipants(List.of(p1, p2)); // 2 active = full for max 2

            when(invitationRepository.findByToken("test-token-uuid")).thenReturn(Optional.of(invitation));
            when(lessonRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(groupLesson));

            assertThatThrownBy(() -> groupLessonService.joinWithCredit(2L, "test-token-uuid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already full");
        }

        @Test
        @DisplayName("Should reject if invitation expired")
        void shouldRejectExpiredInvitation() {
            invitation.setExpiresAt(LocalDateTime.now().minusDays(1));

            when(invitationRepository.findByToken("test-token-uuid")).thenReturn(Optional.of(invitation));

            assertThatThrownBy(() -> groupLessonService.joinWithCredit(2L, "test-token-uuid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expired");
        }
    }

    // ─── JOIN AFTER STRIPE PAYMENT ────────────────────────────

    @Nested
    @DisplayName("joinAfterStripePayment")
    class JoinAfterStripePayment {

        @Test
        @DisplayName("Should add participant without wallet deduction")
        void shouldJoinWithoutWalletDeduction() {
            // Given
            groupLesson.setParticipants(new ArrayList<>());
            when(invitationRepository.findByToken("test-token-uuid")).thenReturn(Optional.of(invitation));
            when(lessonRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(groupLesson));
            when(participantRepository.existsByLessonIdAndStudentId(100L, 2L)).thenReturn(false);
            when(userRepository.findById(2L)).thenReturn(Optional.of(student2));
            when(participantRepository.save(any(LessonParticipant.class))).thenAnswer(inv -> inv.getArgument(0));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
            when(participantRepository.countByLessonIdAndStatus(100L, "ACTIVE")).thenReturn(1);
            when(lessonRepository.findStudentLessonsBetween(anyLong(), any(), any())).thenReturn(Collections.emptyList());

            // When
            GroupLessonResponse response = groupLessonService.joinAfterStripePayment(2L, "test-token-uuid");

            // Then
            assertThat(response).isNotNull();
            // Wallet should NOT be deducted
            verify(walletService, never()).checkAndDeductCredit(anyLong(), anyInt());
            verify(walletService, never()).linkDeductionToLesson(anyLong(), any(), anyInt());

            // Payment record should use ONE_TIME_LESSON
            ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentRepository).save(paymentCaptor.capture());
            assertThat(paymentCaptor.getValue().getPaymentType()).isEqualTo(PaymentType.ONE_TIME_LESSON);
        }
    }

    // ─── CANCEL PARTICIPANT ───────────────────────────────────

    @Nested
    @DisplayName("cancelParticipant")
    class CancelParticipant {

        private LessonParticipant activeParticipant;

        @BeforeEach
        void setUpParticipant() {
            activeParticipant = new LessonParticipant();
            activeParticipant.setId(200L);
            activeParticipant.setLesson(groupLesson);
            activeParticipant.setStudent(student2);
            activeParticipant.setRole("PARTICIPANT");
            activeParticipant.setStatus("ACTIVE");
            activeParticipant.setPricePaidCents(3000);
            activeParticipant.setCommissionCents(375);
        }

        @Test
        @DisplayName("Should cancel and refund 100% if more than 24h before lesson")
        void shouldCancelAndFullRefund() {
            // Given - lesson is 3 days from now, so > 24h
            when(lessonRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(groupLesson));
            when(participantRepository.findActiveByLessonIdAndStudentId(100L, 2L)).thenReturn(Optional.of(activeParticipant));
            when(participantRepository.countByLessonIdAndStatus(100L, "ACTIVE")).thenReturn(1);
            when(paymentRepository.findByLessonIdAndPayerId(100L, 2L)).thenReturn(Optional.of(new Payment()));

            // When
            groupLessonService.cancelParticipant(100L, 2L, "Changed my mind");

            // Then
            assertThat(activeParticipant.getStatus()).isEqualTo("CANCELLED");
            assertThat(activeParticipant.getCancelledBy()).isEqualTo("STUDENT");
            assertThat(activeParticipant.getRefundPercentage()).isEqualTo(100);
            assertThat(activeParticipant.getRefundedAmountCents()).isEqualTo(3000);
            verify(walletService).refundCreditForLesson(2L, groupLesson, 3000, 100);
        }

        @Test
        @DisplayName("Should reopen group if it was FULL")
        void shouldReopenIfFull() {
            groupLesson.setGroupStatus("FULL");
            when(lessonRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(groupLesson));
            when(participantRepository.findActiveByLessonIdAndStudentId(100L, 2L)).thenReturn(Optional.of(activeParticipant));
            when(participantRepository.countByLessonIdAndStatus(100L, "ACTIVE")).thenReturn(1);
            when(paymentRepository.findByLessonIdAndPayerId(100L, 2L)).thenReturn(Optional.empty());

            groupLessonService.cancelParticipant(100L, 2L, null);

            assertThat(groupLesson.getGroupStatus()).isEqualTo("OPEN");
        }

        @Test
        @DisplayName("Should cancel entire lesson if 0 participants remaining")
        void shouldCancelLessonIfNoParticipants() {
            when(lessonRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(groupLesson));
            when(participantRepository.findActiveByLessonIdAndStudentId(100L, 2L)).thenReturn(Optional.of(activeParticipant));
            when(participantRepository.countByLessonIdAndStatus(100L, "ACTIVE")).thenReturn(0);
            when(paymentRepository.findByLessonIdAndPayerId(100L, 2L)).thenReturn(Optional.empty());

            groupLessonService.cancelParticipant(100L, 2L, null);

            assertThat(groupLesson.getStatus()).isEqualTo(LessonStatus.CANCELLED);
            assertThat(groupLesson.getCancelledBy()).isEqualTo("SYSTEM");
        }

        @Test
        @DisplayName("Should reject if not a participant")
        void shouldRejectNotParticipant() {
            when(lessonRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(groupLesson));
            when(participantRepository.findActiveByLessonIdAndStudentId(100L, 2L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> groupLessonService.cancelParticipant(100L, 2L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not a participant");
        }

        @Test
        @DisplayName("Should reject if not a group lesson")
        void shouldRejectNotGroupLesson() {
            groupLesson.setIsGroupLesson(false);
            when(lessonRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(groupLesson));

            assertThatThrownBy(() -> groupLessonService.cancelParticipant(100L, 2L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Not a group lesson");
        }
    }

    // ─── RESOLVE DEADLINE ─────────────────────────────────────

    @Nested
    @DisplayName("resolveDeadline")
    class ResolveDeadline {

        private LessonParticipant creatorParticipant;

        @BeforeEach
        void setUpCreator() {
            groupLesson.setGroupStatus("DEADLINE_PASSED");
            creatorParticipant = new LessonParticipant();
            creatorParticipant.setId(300L);
            creatorParticipant.setLesson(groupLesson);
            creatorParticipant.setStudent(student);
            creatorParticipant.setRole("CREATOR");
            creatorParticipant.setStatus("ACTIVE");
            creatorParticipant.setPricePaidCents(3000); // 60% of 5000
            creatorParticipant.setCommissionCents(375);
        }

        @Test
        @DisplayName("CANCEL should refund everyone 100%")
        void cancelShouldRefundEveryone() {
            // Given
            when(lessonRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(groupLesson));
            when(participantRepository.findActiveByLessonIdAndStudentId(100L, 1L)).thenReturn(Optional.of(creatorParticipant));
            when(participantRepository.findByLessonIdAndStatus(100L, "ACTIVE")).thenReturn(List.of(creatorParticipant));
            when(paymentRepository.findByLessonIdAndPayerId(anyLong(), anyLong())).thenReturn(Optional.empty());

            // When
            groupLessonService.resolveDeadline(100L, 1L, "CANCEL");

            // Then
            assertThat(groupLesson.getStatus()).isEqualTo(LessonStatus.CANCELLED);
            assertThat(groupLesson.getCancelledBy()).isEqualTo("STUDENT");
            verify(walletService).refundCreditForLesson(1L, groupLesson, 3000, 100);
        }

        @Test
        @DisplayName("PAY_FULL should charge difference and convert to private lesson")
        void payFullShouldConvertToPrivate() {
            // Given
            when(lessonRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(groupLesson));
            when(participantRepository.findActiveByLessonIdAndStudentId(100L, 1L)).thenReturn(Optional.of(creatorParticipant));
            when(participantRepository.findByLessonIdAndStatus(100L, "ACTIVE")).thenReturn(List.of(creatorParticipant));

            // When
            groupLessonService.resolveDeadline(100L, 1L, "PAY_FULL");

            // Then: creator paid 3000, teacher rate is 5000, difference = 2000
            verify(walletService).checkAndDeductCredit(1L, 2000);
            verify(walletService).linkDeductionToLesson(1L, groupLesson, 2000);

            // Lesson converted to private
            assertThat(groupLesson.getIsGroupLesson()).isFalse();
            assertThat(groupLesson.getMaxParticipants()).isEqualTo(1);
            assertThat(groupLesson.getGroupStatus()).isNull();

            // Creator's paid amount updated
            assertThat(creatorParticipant.getPricePaidCents()).isEqualTo(5000);
        }

        @Test
        @DisplayName("Should reject if deadline not passed")
        void shouldRejectIfDeadlineNotPassed() {
            groupLesson.setGroupStatus("OPEN");
            when(lessonRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(groupLesson));

            assertThatThrownBy(() -> groupLessonService.resolveDeadline(100L, 1L, "CANCEL"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Deadline has not passed yet");
        }

        @Test
        @DisplayName("Should reject if not the creator")
        void shouldRejectNonCreator() {
            LessonParticipant regularParticipant = new LessonParticipant();
            regularParticipant.setRole("PARTICIPANT");
            regularParticipant.setStatus("ACTIVE");

            when(lessonRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(groupLesson));
            when(participantRepository.findActiveByLessonIdAndStudentId(100L, 2L)).thenReturn(Optional.of(regularParticipant));

            assertThatThrownBy(() -> groupLessonService.resolveDeadline(100L, 2L, "CANCEL"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only the creator");
        }

        @Test
        @DisplayName("Should reject invalid choice")
        void shouldRejectInvalidChoice() {
            when(lessonRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(groupLesson));
            when(participantRepository.findActiveByLessonIdAndStudentId(100L, 1L)).thenReturn(Optional.of(creatorParticipant));

            assertThatThrownBy(() -> groupLessonService.resolveDeadline(100L, 1L, "INVALID"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid choice");
        }
    }

    // ─── HANDLE GROUP LESSON COMPLETION ───────────────────────

    @Nested
    @DisplayName("handleGroupLessonCompletion")
    class HandleGroupLessonCompletion {

        @Test
        @DisplayName("Should calculate earnings and credit teacher")
        void shouldCalculateEarningsAndCreditTeacher() {
            // Given: 2 participants, each paid 3000 cents
            LessonParticipant p1 = new LessonParticipant();
            p1.setPricePaidCents(3000);
            p1.setStudent(student);
            LessonParticipant p2 = new LessonParticipant();
            p2.setPricePaidCents(3000);
            p2.setStudent(student2);

            when(participantRepository.findByLessonIdAndStatus(100L, "ACTIVE")).thenReturn(List.of(p1, p2));
            when(progressRepository.findByStudentId(anyLong())).thenReturn(Optional.empty());

            // When
            groupLessonService.handleGroupLessonCompletion(groupLesson);

            // Then
            // Total collected = 6000
            // Commission = 6000 * 125 / 1000 = 750
            // Teacher earnings = 6000 - 750 = 5250
            assertThat(groupLesson.getCommissionCents()).isEqualTo(750);
            assertThat(groupLesson.getTeacherEarningsCents()).isEqualTo(5250);

            verify(teacherBalanceService).creditEarningsForCompletedLesson(groupLesson);
            assertThat(groupLesson.getEarningsCredited()).isTrue();
            verify(lessonRepository).save(groupLesson);
        }

        @Test
        @DisplayName("Should not double-credit if already credited")
        void shouldNotDoubleCreditTeacher() {
            // Given
            groupLesson.setEarningsCredited(true);
            LessonParticipant p1 = new LessonParticipant();
            p1.setPricePaidCents(3000);
            p1.setStudent(student);

            when(participantRepository.findByLessonIdAndStatus(100L, "ACTIVE")).thenReturn(List.of(p1));
            when(progressRepository.findByStudentId(anyLong())).thenReturn(Optional.empty());

            // When
            groupLessonService.handleGroupLessonCompletion(groupLesson);

            // Then
            verify(teacherBalanceService, never()).creditEarningsForCompletedLesson(any());
        }

        @Test
        @DisplayName("Should update progress for each participant")
        void shouldUpdateProgressForEachParticipant() {
            // Given
            LessonParticipant p1 = new LessonParticipant();
            p1.setPricePaidCents(3000);
            p1.setStudent(student);
            LessonParticipant p2 = new LessonParticipant();
            p2.setPricePaidCents(3000);
            p2.setStudent(student2);

            Progress progress1 = mock(Progress.class);
            Progress progress2 = mock(Progress.class);

            when(participantRepository.findByLessonIdAndStatus(100L, "ACTIVE")).thenReturn(List.of(p1, p2));
            when(progressRepository.findByStudentId(1L)).thenReturn(Optional.of(progress1));
            when(progressRepository.findByStudentId(2L)).thenReturn(Optional.of(progress2));

            // When
            groupLessonService.handleGroupLessonCompletion(groupLesson);

            // Then
            verify(progress1).recordCompletedLesson();
            verify(progress2).recordCompletedLesson();
            verify(progressRepository).save(progress1);
            verify(progressRepository).save(progress2);
        }
    }

    // ─── GET GROUP LESSON DETAILS ─────────────────────────────

    @Nested
    @DisplayName("getGroupLessonDetails")
    class GetGroupLessonDetails {

        @Test
        @DisplayName("Should return details for participant")
        void shouldReturnDetailsForParticipant() {
            // Given
            groupLesson.setParticipants(new ArrayList<>());
            when(lessonRepository.findById(100L)).thenReturn(Optional.of(groupLesson));
            when(userRepository.findById(2L)).thenReturn(Optional.of(student2));
            when(participantRepository.existsActiveByLessonIdAndStudentId(100L, 2L)).thenReturn(true);
            when(invitationRepository.findByLessonId(100L)).thenReturn(Optional.of(invitation));

            // When
            GroupLessonResponse response = groupLessonService.getGroupLessonDetails(100L, 2L);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.isGroupLesson()).isTrue();
            assertThat(response.pricePerPersonCents()).isEqualTo(3000);
        }

        @Test
        @DisplayName("Should reject if not authorized")
        void shouldRejectUnauthorized() {
            User randomUser = new User();
            randomUser.setId(99L);
            randomUser.setRole(UserRole.STUDENT);

            when(lessonRepository.findById(100L)).thenReturn(Optional.of(groupLesson));
            when(userRepository.findById(99L)).thenReturn(Optional.of(randomUser));
            when(participantRepository.existsActiveByLessonIdAndStudentId(100L, 99L)).thenReturn(false);

            assertThatThrownBy(() -> groupLessonService.getGroupLessonDetails(100L, 99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Not authorized");
        }
    }
}
