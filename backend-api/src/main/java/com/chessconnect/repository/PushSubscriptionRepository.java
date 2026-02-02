package com.chessconnect.repository;

import com.chessconnect.model.PushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {

    /**
     * Find all subscriptions for a user.
     */
    List<PushSubscription> findByUserId(Long userId);

    /**
     * Find a subscription by its endpoint.
     */
    Optional<PushSubscription> findByEndpoint(String endpoint);

    /**
     * Delete a subscription by its endpoint.
     */
    @Modifying
    @Query("DELETE FROM PushSubscription p WHERE p.endpoint = :endpoint")
    void deleteByEndpoint(String endpoint);

    /**
     * Delete all subscriptions for a user.
     */
    @Modifying
    @Query("DELETE FROM PushSubscription p WHERE p.user.id = :userId")
    void deleteByUserId(Long userId);

    /**
     * Check if a subscription exists for an endpoint.
     */
    boolean existsByEndpoint(String endpoint);

    /**
     * Count subscriptions for a user.
     */
    long countByUserId(Long userId);
}
