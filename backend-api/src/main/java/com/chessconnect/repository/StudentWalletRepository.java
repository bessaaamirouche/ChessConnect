package com.chessconnect.repository;

import com.chessconnect.model.StudentWallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentWalletRepository extends JpaRepository<StudentWallet, Long> {
    Optional<StudentWallet> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM StudentWallet w WHERE w.user.id = :userId")
    Optional<StudentWallet> findByUserIdForUpdate(Long userId);
}
