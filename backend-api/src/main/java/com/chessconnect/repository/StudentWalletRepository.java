package com.chessconnect.repository;

import com.chessconnect.model.StudentWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentWalletRepository extends JpaRepository<StudentWallet, Long> {
    Optional<StudentWallet> findByUserId(Long userId);
}
