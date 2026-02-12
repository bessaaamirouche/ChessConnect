package com.chessconnect.dto.lesson;

import com.chessconnect.model.Lesson;
import com.chessconnect.model.LessonParticipant;
import com.chessconnect.model.User;
import com.chessconnect.model.enums.LessonStatus;
import com.chessconnect.model.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("LessonResponse Tests")
class LessonResponseTest {

    private User student;
    private User teacher;

    @BeforeEach
    void setUp() {
        student = new User();
        student.setId(1L);
        student.setFirstName("Adam");
        student.setLastName("Test");
        student.setRole(UserRole.STUDENT);

        teacher = new User();
        teacher.setId(10L);
        teacher.setFirstName("Prof");
        teacher.setLastName("Un");
        teacher.setRole(UserRole.TEACHER);
        teacher.setHourlyRateCents(3000);
    }

    private Lesson buildPrivateLesson() {
        Lesson lesson = new Lesson();
        lesson.setId(1L);
        lesson.setStudent(student);
        lesson.setTeacher(teacher);
        lesson.setScheduledAt(LocalDateTime.now().plusDays(1));
        lesson.setDurationMinutes(60);
        lesson.setStatus(LessonStatus.PENDING);
        lesson.setPriceCents(3000);
        lesson.setCommissionCents(375);
        lesson.setTeacherEarningsCents(2625);
        lesson.setIsGroupLesson(false);
        lesson.setIsFromSubscription(false);
        lesson.setParticipants(new ArrayList<>());
        return lesson;
    }

    private Lesson buildGroupLesson(int maxParticipants) {
        Lesson lesson = buildPrivateLesson();
        lesson.setIsGroupLesson(true);
        lesson.setMaxParticipants(maxParticipants);
        lesson.setGroupStatus("OPEN");
        return lesson;
    }

    private LessonParticipant buildParticipant(Lesson lesson, User student, String role, int pricePaid, int commission) {
        LessonParticipant p = new LessonParticipant();
        p.setLesson(lesson);
        p.setStudent(student);
        p.setRole(role);
        p.setStatus("ACTIVE");
        p.setPricePaidCents(pricePaid);
        p.setCommissionCents(commission);
        return p;
    }

    // ─── PRIVATE LESSON ─────────────────────────────────────

    @Nested
    @DisplayName("Private lesson")
    class PrivateLessonTests {

        @Test
        @DisplayName("Should use lesson entity fields directly for private lesson")
        void shouldUseLessonFieldsForPrivateLesson() {
            Lesson lesson = buildPrivateLesson();

            LessonResponse response = LessonResponse.from(lesson);

            assertThat(response.priceCents()).isEqualTo(3000);
            assertThat(response.commissionCents()).isEqualTo(375);
            assertThat(response.teacherEarningsCents()).isEqualTo(2625);
            assertThat(response.isGroupLesson()).isNull();
            assertThat(response.maxParticipants()).isNull();
            assertThat(response.participants()).isNull();
        }

        @Test
        @DisplayName("Should set studentId and teacherId correctly")
        void shouldSetIdsCorrectly() {
            Lesson lesson = buildPrivateLesson();

            LessonResponse response = LessonResponse.from(lesson);

            assertThat(response.studentId()).isEqualTo(1L);
            assertThat(response.teacherId()).isEqualTo(10L);
        }
    }

    // ─── GROUP LESSON FINANCES ──────────────────────────────

    @Nested
    @DisplayName("Group lesson finance computation")
    class GroupLessonFinances {

        @Test
        @DisplayName("Should compute total price from 2 participants' payments")
        void shouldComputeTotalFrom2Participants() {
            Lesson lesson = buildGroupLesson(2);
            // Teacher rate is 3000, but each participant pays 1800 (60% of 3000)
            // Total = 2 * 1800 = 3600
            User student2 = new User();
            student2.setId(2L);
            student2.setFirstName("Lamine");
            student2.setLastName("Test");
            student2.setRole(UserRole.STUDENT);

            LessonParticipant p1 = buildParticipant(lesson, student, "CREATOR", 1800, 225);
            LessonParticipant p2 = buildParticipant(lesson, student2, "PARTICIPANT", 1800, 225);
            lesson.setParticipants(List.of(p1, p2));

            LessonResponse response = LessonResponse.from(lesson);

            // Price should be total collected (3600), not teacher rate (3000)
            assertThat(response.priceCents()).isEqualTo(3600);
            // Commission should be sum of participant commissions (225 + 225 = 450)
            assertThat(response.commissionCents()).isEqualTo(450);
            // Earnings = total - commission = 3600 - 450 = 3150
            assertThat(response.teacherEarningsCents()).isEqualTo(3150);
        }

        @Test
        @DisplayName("Should compute total price from 3 participants' payments")
        void shouldComputeTotalFrom3Participants() {
            Lesson lesson = buildGroupLesson(3);
            lesson.setPriceCents(5000); // Teacher rate 50€
            // Each pays 45% of 5000 = 2250
            User student2 = new User();
            student2.setId(2L);
            student2.setFirstName("B");
            student2.setLastName("T");
            student2.setRole(UserRole.STUDENT);
            User student3 = new User();
            student3.setId(3L);
            student3.setFirstName("C");
            student3.setLastName("T");
            student3.setRole(UserRole.STUDENT);

            int commission = 2250 * 125 / 1000; // 281
            LessonParticipant p1 = buildParticipant(lesson, student, "CREATOR", 2250, commission);
            LessonParticipant p2 = buildParticipant(lesson, student2, "PARTICIPANT", 2250, commission);
            LessonParticipant p3 = buildParticipant(lesson, student3, "PARTICIPANT", 2250, commission);
            lesson.setParticipants(List.of(p1, p2, p3));

            LessonResponse response = LessonResponse.from(lesson);

            // Total = 3 * 2250 = 6750
            assertThat(response.priceCents()).isEqualTo(6750);
            // Commission = 3 * 281 = 843
            assertThat(response.commissionCents()).isEqualTo(843);
            // Earnings = 6750 - 843 = 5907
            assertThat(response.teacherEarningsCents()).isEqualTo(5907);
        }

        @Test
        @DisplayName("Should fall back to lesson fields when no active participants")
        void shouldFallBackWhenNoParticipants() {
            Lesson lesson = buildGroupLesson(2);
            lesson.setPriceCents(3000);
            lesson.setCommissionCents(375);
            lesson.setTeacherEarningsCents(2625);
            lesson.setParticipants(new ArrayList<>());

            LessonResponse response = LessonResponse.from(lesson);

            // No active participants → fall back to lesson entity fields
            assertThat(response.priceCents()).isEqualTo(3000);
            assertThat(response.commissionCents()).isEqualTo(375);
            assertThat(response.teacherEarningsCents()).isEqualTo(2625);
        }

        @Test
        @DisplayName("Should fall back when all participants are cancelled")
        void shouldFallBackWhenAllCancelled() {
            Lesson lesson = buildGroupLesson(2);
            lesson.setPriceCents(3000);
            lesson.setCommissionCents(375);
            lesson.setTeacherEarningsCents(2625);

            LessonParticipant cancelled = new LessonParticipant();
            cancelled.setLesson(lesson);
            cancelled.setStudent(student);
            cancelled.setRole("CREATOR");
            cancelled.setStatus("CANCELLED"); // Not ACTIVE
            cancelled.setPricePaidCents(1800);
            cancelled.setCommissionCents(225);
            lesson.setParticipants(List.of(cancelled));

            LessonResponse response = LessonResponse.from(lesson);

            // No active participants → fall back
            assertThat(response.priceCents()).isEqualTo(3000);
            assertThat(response.commissionCents()).isEqualTo(375);
        }

        @Test
        @DisplayName("Should only count active participants in finance calculation")
        void shouldOnlyCountActiveParticipants() {
            Lesson lesson = buildGroupLesson(2);
            User student2 = new User();
            student2.setId(2L);
            student2.setFirstName("Lamine");
            student2.setLastName("T");
            student2.setRole(UserRole.STUDENT);

            LessonParticipant active = buildParticipant(lesson, student, "CREATOR", 1800, 225);
            LessonParticipant cancelled = new LessonParticipant();
            cancelled.setLesson(lesson);
            cancelled.setStudent(student2);
            cancelled.setRole("PARTICIPANT");
            cancelled.setStatus("CANCELLED");
            cancelled.setPricePaidCents(1800);
            cancelled.setCommissionCents(225);
            lesson.setParticipants(List.of(active, cancelled));

            LessonResponse response = LessonResponse.from(lesson);

            // Only 1 active participant (1800 paid)
            assertThat(response.priceCents()).isEqualTo(1800);
            assertThat(response.commissionCents()).isEqualTo(225);
            assertThat(response.teacherEarningsCents()).isEqualTo(1575);
        }
    }

    // ─── GROUP LESSON METADATA ──────────────────────────────

    @Nested
    @DisplayName("Group lesson metadata")
    class GroupLessonMetadata {

        @Test
        @DisplayName("Should set isGroupLesson to true for group lessons")
        void shouldSetIsGroupLesson() {
            Lesson lesson = buildGroupLesson(2);
            LessonParticipant p = buildParticipant(lesson, student, "CREATOR", 1800, 225);
            lesson.setParticipants(List.of(p));

            LessonResponse response = LessonResponse.from(lesson);

            assertThat(response.isGroupLesson()).isTrue();
            assertThat(response.maxParticipants()).isEqualTo(2);
            assertThat(response.groupStatus()).isEqualTo("OPEN");
        }

        @Test
        @DisplayName("Should include participant summaries for group lessons")
        void shouldIncludeParticipantSummaries() {
            Lesson lesson = buildGroupLesson(2);
            User student2 = new User();
            student2.setId(2L);
            student2.setFirstName("Lamine");
            student2.setLastName("T");
            student2.setRole(UserRole.STUDENT);

            LessonParticipant p1 = buildParticipant(lesson, student, "CREATOR", 1800, 225);
            LessonParticipant p2 = buildParticipant(lesson, student2, "PARTICIPANT", 1800, 225);
            lesson.setParticipants(List.of(p1, p2));

            LessonResponse response = LessonResponse.from(lesson);

            assertThat(response.participants()).hasSize(2);
            assertThat(response.participants().get(0).role()).isEqualTo("CREATOR");
            assertThat(response.participants().get(1).role()).isEqualTo("PARTICIPANT");
        }

        @Test
        @DisplayName("Should set currentParticipantCount from active participants")
        void shouldSetCurrentParticipantCount() {
            Lesson lesson = buildGroupLesson(3);
            LessonParticipant p1 = buildParticipant(lesson, student, "CREATOR", 2250, 281);
            lesson.setParticipants(List.of(p1));

            LessonResponse response = LessonResponse.from(lesson);

            assertThat(response.currentParticipantCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should set invitationToken to null (set by caller)")
        void shouldSetInvitationTokenToNull() {
            Lesson lesson = buildGroupLesson(2);
            LessonParticipant p = buildParticipant(lesson, student, "CREATOR", 1800, 225);
            lesson.setParticipants(List.of(p));

            LessonResponse response = LessonResponse.from(lesson);

            assertThat(response.invitationToken()).isNull();
        }
    }
}
