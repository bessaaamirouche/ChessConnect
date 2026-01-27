package com.chessconnect.repository;

import com.chessconnect.model.Progress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProgressRepository extends JpaRepository<Progress, Long> {

    Optional<Progress> findByStudentId(Long studentId);

    @Modifying
    void deleteByStudentId(Long studentId);
}
