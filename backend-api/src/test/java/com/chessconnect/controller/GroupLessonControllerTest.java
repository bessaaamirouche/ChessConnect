package com.chessconnect.controller;

import com.chessconnect.dto.group.*;
import com.chessconnect.dto.lesson.LessonResponse;
import com.chessconnect.dto.payment.CheckoutSessionResponse;
import com.chessconnect.model.Payment;
import com.chessconnect.model.User;
import com.chessconnect.model.enums.LessonStatus;
import com.chessconnect.model.enums.PaymentType;
import com.chessconnect.model.enums.UserRole;
import com.chessconnect.repository.GroupInvitationRepository;
import com.chessconnect.repository.LessonRepository;
import com.chessconnect.repository.PaymentRepository;
import com.chessconnect.repository.UserRepository;
import com.chessconnect.security.UserDetailsImpl;
import com.chessconnect.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GroupLessonController Tests")
class GroupLessonControllerTest {

    @Mock private GroupLessonService groupLessonService;
    @Mock private StripeService stripeService;
    @Mock private WalletService walletService;
    @Mock private InvoiceService invoiceService;
    @Mock private UserRepository userRepository;
    @Mock private GroupInvitationRepository invitationRepository;
    @Mock private LessonRepository lessonRepository;
    @Mock private PaymentRepository paymentRepository;

    @InjectMocks
    private GroupLessonController controller;

    private UserDetailsImpl studentDetails;
    private User student;
    private User teacher;

    @BeforeEach
    void setUp() {
        student = new User();
        student.setId(1L);
        student.setFirstName("Denis");
        student.setLastName("Benoit");
        student.setEmail("denis@test.com");
        student.setPassword("password");
        student.setRole(UserRole.STUDENT);

        teacher = new User();
        teacher.setId(10L);
        teacher.setFirstName("Samuel");
        teacher.setLastName("Benis");
        teacher.setRole(UserRole.TEACHER);
        teacher.setHourlyRateCents(5000);

        studentDetails = new UserDetailsImpl(student);
    }

    private GroupLessonResponse buildMockResponse(Long lessonId, String token) {
        LessonResponse lessonResponse = new LessonResponse(
                lessonId,                           // id
                1L,                                 // studentId
                "Denis B.",                         // studentName
                null,                               // studentLevel
                null,                               // studentAge
                null,                               // studentElo
                10L,                                // teacherId
                "Samuel B.",                        // teacherName
                LocalDateTime.now().plusDays(3),     // scheduledAt
                60,                                 // durationMinutes
                null,                               // zoomLink
                LessonStatus.PENDING,               // status
                5000,                               // priceCents
                null,                               // commissionCents
                null,                               // teacherEarningsCents
                false,                              // isFromSubscription
                null,                               // notes
                null,                               // cancellationReason
                null,                               // cancelledBy
                null,                               // refundPercentage
                null,                               // refundedAmountCents
                null,                               // teacherObservations
                null,                               // teacherComment
                null,                               // teacherCommentAt
                null,                               // recordingUrl
                null,                               // teacherJoinedAt
                null,                               // createdAt
                null,                               // courseId
                null,                               // courseTitle
                null,                               // courseGrade
                true,                               // isGroupLesson
                2,                                  // maxParticipants
                "OPEN",                             // groupStatus
                1,                                  // currentParticipantCount
                null,                               // invitationToken
                Collections.emptyList()             // participants
        );
        return new GroupLessonResponse(
                lessonResponse, true, 2, "OPEN", 1,
                3000, token, LocalDateTime.now().plusDays(2),
                Collections.emptyList()
        );
    }

    // ─── CREATE ───────────────────────────────────────────────

    @Nested
    @DisplayName("POST / (create)")
    class Create {

        @Test
        @DisplayName("Should return success with lessonId and invitationToken")
        void shouldCreateSuccessfully() {
            // Given
            BookGroupLessonRequest request = new BookGroupLessonRequest(
                    10L, LocalDateTime.now().plusDays(3), 60, null, 2, null);

            when(userRepository.findById(10L)).thenReturn(java.util.Optional.of(teacher));
            GroupLessonResponse mockResponse = buildMockResponse(100L, "abc-token");
            when(groupLessonService.createGroupLesson(1L, request)).thenReturn(mockResponse);

            com.chessconnect.model.Lesson lesson = new com.chessconnect.model.Lesson();
            lesson.setId(100L);
            when(lessonRepository.findById(100L)).thenReturn(java.util.Optional.of(lesson));
            when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(student));
            when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(walletService.getBalance(1L)).thenReturn(2000);

            // When
            ResponseEntity<Map<String, Object>> response = controller.create(studentDetails, request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).containsEntry("success", true);
            assertThat(response.getBody()).containsKey("lessonId");
            assertThat(response.getBody()).containsKey("invitationToken");
        }

        @Test
        @DisplayName("Should return 400 on error")
        void shouldReturn400OnError() {
            // Use valid group size so pricing doesn't throw; error comes from wallet
            BookGroupLessonRequest request = new BookGroupLessonRequest(
                    10L, LocalDateTime.now().plusDays(3), 60, null, 2, null);

            when(userRepository.findById(10L)).thenReturn(java.util.Optional.of(teacher));
            doThrow(new RuntimeException("Insufficient balance"))
                    .when(walletService).checkAndDeductCredit(anyLong(), anyInt());

            ResponseEntity<Map<String, Object>> response = controller.create(studentDetails, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).containsEntry("success", false);
        }
    }

    // ─── GET INVITATION ───────────────────────────────────────

    @Nested
    @DisplayName("GET /invitation/{token}")
    class GetInvitation {

        @Test
        @DisplayName("Should return invitation details")
        void shouldReturnInvitationDetails() {
            GroupInvitationResponse mockResponse = new GroupInvitationResponse(
                    "abc-token", 100L, "Samuel", "B.", null,
                    LocalDateTime.now().plusDays(3), 60, 2, 1, 1,
                    3000, LocalDateTime.now().plusDays(2), false, false,
                    Collections.emptyList()
            );
            when(groupLessonService.getInvitationDetails("abc-token")).thenReturn(mockResponse);

            ResponseEntity<GroupInvitationResponse> response = controller.getInvitation("abc-token");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().token()).isEqualTo("abc-token");
            assertThat(response.getBody().pricePerPersonCents()).isEqualTo(3000);
        }

        @Test
        @DisplayName("Should return 404 for invalid token")
        void shouldReturn404ForInvalidToken() {
            when(groupLessonService.getInvitationDetails("bad-token"))
                    .thenThrow(new IllegalArgumentException("Invitation not found"));

            ResponseEntity<GroupInvitationResponse> response = controller.getInvitation("bad-token");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    // ─── JOIN WITH CREDIT ─────────────────────────────────────

    @Nested
    @DisplayName("POST /join/credit")
    class JoinWithCredit {

        @Test
        @DisplayName("Should return success")
        void shouldJoinSuccessfully() {
            JoinGroupLessonRequest request = new JoinGroupLessonRequest("abc-token", true, null);
            GroupLessonResponse mockResponse = buildMockResponse(100L, "abc-token");

            when(groupLessonService.joinWithCredit(1L, "abc-token")).thenReturn(mockResponse);
            when(walletService.getBalance(1L)).thenReturn(2000);

            ResponseEntity<Map<String, Object>> response = controller.joinWithCredit(studentDetails, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).containsEntry("success", true);
            assertThat(response.getBody()).containsKey("lessonId");
        }

        @Test
        @DisplayName("Should return 400 on error")
        void shouldReturn400OnError() {
            JoinGroupLessonRequest request = new JoinGroupLessonRequest("abc-token", true, null);
            when(groupLessonService.joinWithCredit(1L, "abc-token"))
                    .thenThrow(new RuntimeException("Insufficient balance"));

            ResponseEntity<Map<String, Object>> response = controller.joinWithCredit(studentDetails, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).containsEntry("success", false);
            assertThat(response.getBody()).containsEntry("error", "Insufficient balance");
        }
    }

    // ─── LEAVE ────────────────────────────────────────────────

    @Nested
    @DisplayName("DELETE /{id}/leave")
    class Leave {

        @Test
        @DisplayName("Should return success")
        void shouldLeaveSuccessfully() {
            doNothing().when(groupLessonService).cancelParticipant(100L, 1L, "reason");

            ResponseEntity<Map<String, Object>> response = controller.leave(100L, studentDetails, "reason");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).containsEntry("success", true);
        }
    }

    // ─── RESOLVE DEADLINE ─────────────────────────────────────

    @Nested
    @DisplayName("POST /{id}/resolve-deadline")
    class ResolveDeadlineTests {

        @Test
        @DisplayName("CANCEL should return cancellation message")
        void cancelShouldReturnCancellationMessage() {
            ResolveDeadlineRequest request = new ResolveDeadlineRequest("CANCEL");
            doNothing().when(groupLessonService).resolveDeadline(100L, 1L, "CANCEL");

            ResponseEntity<Map<String, Object>> response = controller.resolveDeadline(100L, studentDetails, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).containsEntry("success", true);
            assertThat((String) response.getBody().get("message")).contains("annule");
        }

        @Test
        @DisplayName("PAY_FULL should return conversion message")
        void payFullShouldReturnConversionMessage() {
            ResolveDeadlineRequest request = new ResolveDeadlineRequest("PAY_FULL");
            doNothing().when(groupLessonService).resolveDeadline(100L, 1L, "PAY_FULL");

            ResponseEntity<Map<String, Object>> response = controller.resolveDeadline(100L, studentDetails, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).containsEntry("success", true);
            assertThat((String) response.getBody().get("message")).contains("prive");
        }
    }

    // ─── CREATE CHECKOUT (Stripe) ───────────────────────────────

    @Nested
    @DisplayName("POST /checkout (createCheckout)")
    class CreateCheckout {

        @Test
        @DisplayName("Should return checkout session with clientSecret")
        void shouldReturnCheckoutSession() throws Exception {
            BookGroupLessonRequest request = new BookGroupLessonRequest(
                    10L, LocalDateTime.now().plusDays(3), 60, null, 2, null);

            when(userRepository.findById(10L)).thenReturn(java.util.Optional.of(teacher));
            when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(student));

            com.stripe.model.checkout.Session mockSession = mock(com.stripe.model.checkout.Session.class);
            when(mockSession.getId()).thenReturn("cs_test_123");
            when(mockSession.getClientSecret()).thenReturn("cs_secret_abc");
            when(stripeService.createLessonPaymentSession(
                    any(), eq(10L), eq(3000), anyString(), anyString(), eq(60), eq("GROUP_CREATE:2"), eq(true), isNull()
            )).thenReturn(mockSession);
            when(stripeService.getPublishableKey()).thenReturn("pk_test_xyz");

            ResponseEntity<?> response = controller.createCheckout(studentDetails, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            CheckoutSessionResponse body = (CheckoutSessionResponse) response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.getSessionId()).isEqualTo("cs_test_123");
            assertThat(body.getClientSecret()).isEqualTo("cs_secret_abc");
            assertThat(body.getPublishableKey()).isEqualTo("pk_test_xyz");
        }

        @Test
        @DisplayName("Should calculate correct price for group of 3")
        void shouldCalculateCorrectPriceForGroupOf3() throws Exception {
            BookGroupLessonRequest request = new BookGroupLessonRequest(
                    10L, LocalDateTime.now().plusDays(3), 60, null, 3, null);

            when(userRepository.findById(10L)).thenReturn(java.util.Optional.of(teacher));
            when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(student));

            com.stripe.model.checkout.Session mockSession = mock(com.stripe.model.checkout.Session.class);
            when(mockSession.getId()).thenReturn("cs_test");
            when(mockSession.getClientSecret()).thenReturn("cs_secret");
            // 5000 * 45% = 2250 for group of 3
            when(stripeService.createLessonPaymentSession(
                    any(), eq(10L), eq(2250), anyString(), anyString(), eq(60), eq("GROUP_CREATE:3"), eq(true), isNull()
            )).thenReturn(mockSession);
            when(stripeService.getPublishableKey()).thenReturn("pk_test");

            ResponseEntity<?> response = controller.createCheckout(studentDetails, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(stripeService).createLessonPaymentSession(
                    any(), eq(10L), eq(2250), anyString(), anyString(), eq(60), eq("GROUP_CREATE:3"), eq(true), isNull()
            );
        }

        @Test
        @DisplayName("Should return 502 on StripeException")
        void shouldReturn502OnStripeError() throws Exception {
            BookGroupLessonRequest request = new BookGroupLessonRequest(
                    10L, LocalDateTime.now().plusDays(3), 60, null, 2, null);

            when(userRepository.findById(10L)).thenReturn(java.util.Optional.of(teacher));
            when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(student));
            when(stripeService.createLessonPaymentSession(
                    any(), anyLong(), anyInt(), anyString(), anyString(), anyInt(), anyString(), anyBoolean(), any()
            )).thenThrow(new com.stripe.exception.ApiException("Stripe down", null, null, 500, null));

            ResponseEntity<?> response = controller.createCheckout(studentDetails, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        }

        @Test
        @DisplayName("Should return 400 if teacher not found")
        void shouldReturn400IfTeacherNotFound() {
            BookGroupLessonRequest request = new BookGroupLessonRequest(
                    99L, LocalDateTime.now().plusDays(3), 60, null, 2, null);

            when(userRepository.findById(99L)).thenReturn(java.util.Optional.empty());

            ResponseEntity<?> response = controller.createCheckout(studentDetails, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    // ─── CONFIRM CREATE PAYMENT (Stripe) ─────────────────────────

    @Nested
    @DisplayName("POST /create/confirm (confirmCreatePayment)")
    class ConfirmCreatePayment {

        private com.stripe.model.checkout.Session buildMockStripeSession(
                String paymentStatus, String userId, String teacherId,
                String scheduledAt, String duration, String notes, String courseId,
                String paymentIntentId
        ) {
            com.stripe.model.checkout.Session session = mock(com.stripe.model.checkout.Session.class);
            lenient().when(session.getPaymentStatus()).thenReturn(paymentStatus);
            lenient().when(session.getPaymentIntent()).thenReturn(paymentIntentId);
            Map<String, String> metadata = new HashMap<>();
            if (userId != null) metadata.put("user_id", userId);
            if (teacherId != null) metadata.put("teacher_id", teacherId);
            if (scheduledAt != null) metadata.put("scheduled_at", scheduledAt);
            if (duration != null) metadata.put("duration_minutes", duration);
            if (notes != null) metadata.put("notes", notes);
            if (courseId != null) metadata.put("course_id", courseId);
            lenient().when(session.getMetadata()).thenReturn(metadata);
            return session;
        }

        @Test
        @DisplayName("Should create group lesson after successful Stripe payment")
        void shouldCreateGroupLessonAfterPayment() throws Exception {
            LocalDateTime scheduledAt = LocalDateTime.now().plusDays(3);
            com.stripe.model.checkout.Session session = buildMockStripeSession(
                    "paid", "1", "10", scheduledAt.toString(), "60",
                    "GROUP_CREATE:2", null, "pi_test_123"
            );
            when(stripeService.retrieveSession("cs_test")).thenReturn(session);

            GroupLessonResponse mockResponse = buildMockResponse(100L, "invite-token");
            when(groupLessonService.createGroupLesson(eq(1L), any(BookGroupLessonRequest.class))).thenReturn(mockResponse);
            when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(student));
            when(userRepository.findById(10L)).thenReturn(java.util.Optional.of(teacher));

            com.chessconnect.model.Lesson lesson = new com.chessconnect.model.Lesson();
            lesson.setId(100L);
            when(lessonRepository.findById(100L)).thenReturn(java.util.Optional.of(lesson));
            when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ResponseEntity<Map<String, Object>> response = controller.confirmCreatePayment("cs_test");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).containsEntry("success", true);
            assertThat(response.getBody()).containsEntry("invitationToken", "invite-token");
            assertThat(response.getBody()).containsKey("lessonId");

            // Verify payment record with Stripe payment intent
            ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentRepository).save(paymentCaptor.capture());
            Payment savedPayment = paymentCaptor.getValue();
            assertThat(savedPayment.getPaymentType()).isEqualTo(PaymentType.ONE_TIME_LESSON);
            assertThat(savedPayment.getStripePaymentIntentId()).isEqualTo("pi_test_123");
            assertThat(savedPayment.getAmountCents()).isEqualTo(3000); // 60% of 5000

            // Verify invoice generated
            verify(invoiceService).generateInvoicesForPayment(
                    "pi_test_123", 1L, 10L, 100L, 3000, false
            );
        }

        @Test
        @DisplayName("Should return 400 if payment not paid")
        void shouldReturn400IfNotPaid() throws Exception {
            com.stripe.model.checkout.Session session = buildMockStripeSession(
                    "unpaid", "1", "10", LocalDateTime.now().toString(), "60",
                    "GROUP_CREATE:2", null, null
            );
            when(stripeService.retrieveSession("cs_test")).thenReturn(session);

            ResponseEntity<Map<String, Object>> response = controller.confirmCreatePayment("cs_test");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).containsEntry("success", false);
            verify(groupLessonService, never()).createGroupLesson(anyLong(), any());
        }

        @Test
        @DisplayName("Should return 400 if user_id missing from metadata")
        void shouldReturn400IfUserIdMissing() throws Exception {
            com.stripe.model.checkout.Session session = buildMockStripeSession(
                    "paid", null, "10", LocalDateTime.now().toString(), "60",
                    "GROUP_CREATE:2", null, "pi_test"
            );
            when(stripeService.retrieveSession("cs_test")).thenReturn(session);

            ResponseEntity<Map<String, Object>> response = controller.confirmCreatePayment("cs_test");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).containsEntry("error", "Identifiant utilisateur introuvable");
        }

        @Test
        @DisplayName("Should return 400 if notes not GROUP_CREATE format")
        void shouldReturn400IfNotGroupCreateFormat() throws Exception {
            com.stripe.model.checkout.Session session = buildMockStripeSession(
                    "paid", "1", "10", LocalDateTime.now().toString(), "60",
                    "some random notes", null, "pi_test"
            );
            when(stripeService.retrieveSession("cs_test")).thenReturn(session);

            ResponseEntity<Map<String, Object>> response = controller.confirmCreatePayment("cs_test");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).containsEntry("error", "Session invalide pour un cours en groupe");
        }

        @Test
        @DisplayName("Should return 400 if notes is null")
        void shouldReturn400IfNotesNull() throws Exception {
            com.stripe.model.checkout.Session session = buildMockStripeSession(
                    "paid", "1", "10", LocalDateTime.now().toString(), "60",
                    null, null, "pi_test"
            );
            when(stripeService.retrieveSession("cs_test")).thenReturn(session);

            ResponseEntity<Map<String, Object>> response = controller.confirmCreatePayment("cs_test");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).containsEntry("error", "Session invalide pour un cours en groupe");
        }

        @Test
        @DisplayName("Should return 502 on StripeException")
        void shouldReturn502OnStripeError() throws Exception {
            when(stripeService.retrieveSession("cs_test"))
                    .thenThrow(new com.stripe.exception.ApiException("Stripe error", null, null, 500, null));

            ResponseEntity<Map<String, Object>> response = controller.confirmCreatePayment("cs_test");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        }

        @Test
        @DisplayName("Should pass courseId from metadata when present")
        void shouldPassCourseIdWhenPresent() throws Exception {
            LocalDateTime scheduledAt = LocalDateTime.now().plusDays(3);
            com.stripe.model.checkout.Session session = buildMockStripeSession(
                    "paid", "1", "10", scheduledAt.toString(), "60",
                    "GROUP_CREATE:2", "42", "pi_test"
            );
            when(stripeService.retrieveSession("cs_test")).thenReturn(session);

            GroupLessonResponse mockResponse = buildMockResponse(100L, "token");
            when(groupLessonService.createGroupLesson(eq(1L), any(BookGroupLessonRequest.class))).thenReturn(mockResponse);
            when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(student));
            when(userRepository.findById(10L)).thenReturn(java.util.Optional.of(teacher));
            com.chessconnect.model.Lesson lesson = new com.chessconnect.model.Lesson();
            lesson.setId(100L);
            when(lessonRepository.findById(100L)).thenReturn(java.util.Optional.of(lesson));
            when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            controller.confirmCreatePayment("cs_test");

            ArgumentCaptor<BookGroupLessonRequest> reqCaptor =
                    ArgumentCaptor.forClass(BookGroupLessonRequest.class);
            verify(groupLessonService).createGroupLesson(eq(1L), reqCaptor.capture());
            assertThat(reqCaptor.getValue().courseId()).isEqualTo(42L);
            assertThat(reqCaptor.getValue().targetGroupSize()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should not call wallet deduction (Stripe already charged)")
        void shouldNotDeductWallet() throws Exception {
            LocalDateTime scheduledAt = LocalDateTime.now().plusDays(3);
            com.stripe.model.checkout.Session session = buildMockStripeSession(
                    "paid", "1", "10", scheduledAt.toString(), "60",
                    "GROUP_CREATE:2", null, "pi_test"
            );
            when(stripeService.retrieveSession("cs_test")).thenReturn(session);

            GroupLessonResponse mockResponse = buildMockResponse(100L, "token");
            when(groupLessonService.createGroupLesson(eq(1L), any(BookGroupLessonRequest.class))).thenReturn(mockResponse);
            when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(student));
            when(userRepository.findById(10L)).thenReturn(java.util.Optional.of(teacher));
            com.chessconnect.model.Lesson lesson = new com.chessconnect.model.Lesson();
            lesson.setId(100L);
            when(lessonRepository.findById(100L)).thenReturn(java.util.Optional.of(lesson));
            when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            controller.confirmCreatePayment("cs_test");

            // Wallet should never be touched for Stripe payments
            verify(walletService, never()).checkAndDeductCredit(anyLong(), anyInt());
            verify(walletService, never()).linkDeductionToLesson(anyLong(), any(), anyInt());
        }
    }

    // ─── GET DETAILS ──────────────────────────────────────────

    @Nested
    @DisplayName("GET /{id}")
    class GetDetails {

        @Test
        @DisplayName("Should return group lesson details")
        void shouldReturnDetails() {
            GroupLessonResponse mockResponse = buildMockResponse(100L, "abc-token");
            when(groupLessonService.getGroupLessonDetails(100L, 1L)).thenReturn(mockResponse);

            ResponseEntity<GroupLessonResponse> response = controller.getDetails(100L, studentDetails);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isGroupLesson()).isTrue();
        }

        @Test
        @DisplayName("Should return 403 if not authorized")
        void shouldReturn403IfNotAuthorized() {
            when(groupLessonService.getGroupLessonDetails(100L, 1L))
                    .thenThrow(new IllegalArgumentException("Not authorized"));

            ResponseEntity<GroupLessonResponse> response = controller.getDetails(100L, studentDetails);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }
}
