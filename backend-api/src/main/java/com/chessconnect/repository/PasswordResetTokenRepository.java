package com.chessconnect.repository;

import com.chessconnect.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    List<PasswordResetToken> findByUserIdAndUsedAtIsNullAndExpiresAtAfter(Long userId, LocalDateTime now);

    @Modifying
    void deleteByExpiresAtBefore(LocalDateTime dateTime);

    @Modifying
    void deleteByUserId(Long userId);
}
