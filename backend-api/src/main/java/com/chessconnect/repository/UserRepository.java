package com.chessconnect.repository;

import com.chessconnect.model.User;
import com.chessconnect.model.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUuid(String uuid);

    boolean existsByEmail(String email);

    List<User> findByRole(UserRole role);

    Page<User> findByRole(UserRole role, Pageable pageable);

    Page<User> findByRoleNot(UserRole role, Pageable pageable);

    Page<User> findByRoleAndRoleNot(UserRole role, UserRole excludedRole, Pageable pageable);

    List<User> findByRoleAndAcceptsSubscriptionTrue(UserRole role);

    long countByRole(UserRole role);

    /**
     * Count registrations by role per day within a date range.
     * Returns List of [date, count] pairs.
     */
    @Query(value = "SELECT DATE(created_at) as date, COUNT(*) as count " +
           "FROM users WHERE role = :role AND created_at BETWEEN :start AND :end " +
           "GROUP BY DATE(created_at) ORDER BY date", nativeQuery = true)
    List<Object[]> countRegistrationsByRoleAndDay(
            @Param("role") String role,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
