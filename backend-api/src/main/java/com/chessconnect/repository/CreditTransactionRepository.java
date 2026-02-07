package com.chessconnect.repository;

import com.chessconnect.model.CreditTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CreditTransactionRepository extends JpaRepository<CreditTransaction, Long> {
    List<CreditTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);

    boolean existsByStripePaymentIntentId(String stripePaymentIntentId);

    List<CreditTransaction> findByLessonId(Long lessonId);

    void deleteByUserId(Long userId);

    void deleteByLessonId(Long lessonId);

    void deleteByLessonStudentId(Long studentId);

    void deleteByLessonTeacherId(Long teacherId);
}
