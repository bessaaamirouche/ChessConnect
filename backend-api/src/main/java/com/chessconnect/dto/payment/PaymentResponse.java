package com.chessconnect.dto.payment;

import com.chessconnect.model.Payment;
import com.chessconnect.model.enums.PaymentStatus;
import com.chessconnect.model.enums.PaymentType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PaymentResponse {
    private Long id;
    private Long payerId;
    private String payerName;
    private Long teacherId;
    private String teacherName;
    private Long lessonId;
    private Long subscriptionId;
    private PaymentType paymentType;
    private Integer amountCents;
    private Integer commissionCents;
    private Integer teacherPayoutCents;
    private PaymentStatus status;
    private LocalDateTime processedAt;
    private LocalDateTime createdAt;

    public static PaymentResponse fromEntity(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .payerId(payment.getPayer().getId())
                .payerName(payment.getPayer().getFullName())
                .teacherId(payment.getTeacher() != null ? payment.getTeacher().getId() : null)
                .teacherName(payment.getTeacher() != null ? payment.getTeacher().getFullName() : null)
                .lessonId(payment.getLesson() != null ? payment.getLesson().getId() : null)
                .subscriptionId(payment.getSubscription() != null ? payment.getSubscription().getId() : null)
                .paymentType(payment.getPaymentType())
                .amountCents(payment.getAmountCents())
                .commissionCents(payment.getCommissionCents())
                .teacherPayoutCents(payment.getTeacherPayoutCents())
                .status(payment.getStatus())
                .processedAt(payment.getProcessedAt())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
