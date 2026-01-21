package com.chessconnect.repository;

import com.chessconnect.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByStudentIdAndIsActiveTrue(Long studentId);

    boolean existsByStudentIdAndIsActiveTrue(Long studentId);

    @Query("SELECT s FROM Subscription s WHERE s.student.id = :studentId AND s.isActive = true ORDER BY s.createdAt DESC")
    List<Subscription> findActiveSubscriptionsByStudentId(@Param("studentId") Long studentId);

    List<Subscription> findByStudentIdOrderByCreatedAtDesc(Long studentId);

    List<Subscription> findAllByIsActiveTrue();

    Optional<Subscription> findByStripeSubscriptionId(String stripeSubscriptionId);

    long countByIsActiveTrue();

    void deleteByStudentId(Long studentId);
}
