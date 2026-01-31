package com.chessconnect.model;

import com.chessconnect.model.enums.LessonStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "lessons", indexes = {
    @Index(name = "idx_lesson_student_id", columnList = "student_id"),
    @Index(name = "idx_lesson_teacher_id", columnList = "teacher_id"),
    @Index(name = "idx_lesson_status", columnList = "status"),
    @Index(name = "idx_lesson_scheduled_at", columnList = "scheduled_at"),
    @Index(name = "idx_lesson_status_scheduled", columnList = "status, scheduled_at"),
    @Index(name = "idx_lesson_teacher_status", columnList = "teacher_id, status")
})
public class Lesson {

    // Commission: 12.5% total (10% platform + 2.5% Stripe)
    // Using 125/1000 for precise calculation
    public static final int COMMISSION_RATE_NUMERATOR = 125;
    public static final int COMMISSION_RATE_DENOMINATOR = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id")
    private Subscription subscription;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes = 60;

    @Column(name = "zoom_link")
    private String zoomLink;

    @Column(name = "zoom_meeting_id")
    private String zoomMeetingId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LessonStatus status = LessonStatus.PENDING;

    @Column(name = "price_cents", nullable = false)
    private Integer priceCents;

    @Column(name = "commission_cents", nullable = false)
    private Integer commissionCents;

    @Column(name = "teacher_earnings_cents", nullable = false)
    private Integer teacherEarningsCents;

    @Column(name = "is_from_subscription", nullable = false)
    private Boolean isFromSubscription = false;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @Column(name = "cancelled_by")
    private String cancelledBy; // "STUDENT", "TEACHER", "SYSTEM"

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "refund_percentage")
    private Integer refundPercentage; // 0, 50, 100

    @Column(name = "refunded_amount_cents")
    private Integer refundedAmountCents;

    @Column(name = "stripe_refund_id")
    private String stripeRefundId;

    @Column(name = "earnings_credited")
    private Boolean earningsCredited = false;

    @Column(name = "reminder_sent")
    private Boolean reminderSent = false;

    @Column(name = "teacher_observations", columnDefinition = "TEXT")
    private String teacherObservations;

    @Column(name = "teacher_comment", columnDefinition = "TEXT")
    private String teacherComment;

    @Column(name = "teacher_comment_at")
    private LocalDateTime teacherCommentAt;

    @Column(name = "deleted_by_teacher")
    private Boolean deletedByTeacher = false;

    @Column(name = "deleted_by_student")
    private Boolean deletedByStudent = false;

    @Column(name = "recording_url")
    private String recordingUrl;

    @Column(name = "teacher_joined_at")
    private LocalDateTime teacherJoinedAt;

    @Column(name = "is_free_trial")
    private Boolean isFreeTrial = false;

    @Column(name = "free_trial_started_at")
    private LocalDateTime freeTrialStartedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        calculateCommission();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    private void calculateCommission() {
        if (priceCents != null) {
            // 12.5% commission = (price * 125) / 1000
            this.commissionCents = (priceCents * COMMISSION_RATE_NUMERATOR) / COMMISSION_RATE_DENOMINATOR;
            this.teacherEarningsCents = priceCents - commissionCents;
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getStudent() { return student; }
    public void setStudent(User student) { this.student = student; }

    public User getTeacher() { return teacher; }
    public void setTeacher(User teacher) { this.teacher = teacher; }

    public Subscription getSubscription() { return subscription; }
    public void setSubscription(Subscription subscription) { this.subscription = subscription; }

    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(LocalDateTime scheduledAt) { this.scheduledAt = scheduledAt; }

    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }

    public String getZoomLink() { return zoomLink; }
    public void setZoomLink(String zoomLink) { this.zoomLink = zoomLink; }

    public String getZoomMeetingId() { return zoomMeetingId; }
    public void setZoomMeetingId(String zoomMeetingId) { this.zoomMeetingId = zoomMeetingId; }

    public LessonStatus getStatus() { return status; }
    public void setStatus(LessonStatus status) { this.status = status; }

    public Integer getPriceCents() { return priceCents; }
    public void setPriceCents(Integer priceCents) {
        this.priceCents = priceCents;
        calculateCommission();
    }

    public Integer getCommissionCents() { return commissionCents; }
    public Integer getTeacherEarningsCents() { return teacherEarningsCents; }

    public Boolean getIsFromSubscription() { return isFromSubscription; }
    public void setIsFromSubscription(Boolean isFromSubscription) { this.isFromSubscription = isFromSubscription; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }

    public String getCancelledBy() { return cancelledBy; }
    public void setCancelledBy(String cancelledBy) { this.cancelledBy = cancelledBy; }

    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }

    public Integer getRefundPercentage() { return refundPercentage; }
    public void setRefundPercentage(Integer refundPercentage) { this.refundPercentage = refundPercentage; }

    public Integer getRefundedAmountCents() { return refundedAmountCents; }
    public void setRefundedAmountCents(Integer refundedAmountCents) { this.refundedAmountCents = refundedAmountCents; }

    public String getStripeRefundId() { return stripeRefundId; }
    public void setStripeRefundId(String stripeRefundId) { this.stripeRefundId = stripeRefundId; }

    public Boolean getEarningsCredited() { return earningsCredited; }
    public void setEarningsCredited(Boolean earningsCredited) { this.earningsCredited = earningsCredited; }

    public Boolean getReminderSent() { return reminderSent; }
    public void setReminderSent(Boolean reminderSent) { this.reminderSent = reminderSent; }

    public String getTeacherObservations() { return teacherObservations; }
    public void setTeacherObservations(String teacherObservations) { this.teacherObservations = teacherObservations; }

    public String getTeacherComment() { return teacherComment; }
    public void setTeacherComment(String teacherComment) { this.teacherComment = teacherComment; }

    public LocalDateTime getTeacherCommentAt() { return teacherCommentAt; }
    public void setTeacherCommentAt(LocalDateTime teacherCommentAt) { this.teacherCommentAt = teacherCommentAt; }

    public Boolean getDeletedByTeacher() { return deletedByTeacher; }
    public void setDeletedByTeacher(Boolean deletedByTeacher) { this.deletedByTeacher = deletedByTeacher; }

    public Boolean getDeletedByStudent() { return deletedByStudent; }
    public void setDeletedByStudent(Boolean deletedByStudent) { this.deletedByStudent = deletedByStudent; }

    public String getRecordingUrl() { return recordingUrl; }
    public void setRecordingUrl(String recordingUrl) { this.recordingUrl = recordingUrl; }

    public LocalDateTime getTeacherJoinedAt() { return teacherJoinedAt; }
    public void setTeacherJoinedAt(LocalDateTime teacherJoinedAt) { this.teacherJoinedAt = teacherJoinedAt; }

    public Boolean getIsFreeTrial() { return isFreeTrial; }
    public void setIsFreeTrial(Boolean isFreeTrial) { this.isFreeTrial = isFreeTrial; }

    public LocalDateTime getFreeTrialStartedAt() { return freeTrialStartedAt; }
    public void setFreeTrialStartedAt(LocalDateTime freeTrialStartedAt) { this.freeTrialStartedAt = freeTrialStartedAt; }

    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
