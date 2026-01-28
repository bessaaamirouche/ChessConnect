package com.chessconnect.repository;

import com.chessconnect.model.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByToken(String token);

    List<EmailVerificationToken> findByUserIdAndUsedAtIsNullAndExpiresAtAfter(Long userId, LocalDateTime now);

    Optional<EmailVerificationToken> findFirstByUserIdAndUsedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
            Long userId, LocalDateTime now);

    @Modifying
    void deleteByExpiresAtBefore(LocalDateTime dateTime);

    @Modifying
    void deleteByUserId(Long userId);
}
