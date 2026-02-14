package com.chessconnect.repository;

import com.chessconnect.model.PromoCode;
import com.chessconnect.model.enums.PromoCodeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PromoCodeRepository extends JpaRepository<PromoCode, Long> {

    Optional<PromoCode> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    List<PromoCode> findByCodeType(PromoCodeType codeType);

    List<PromoCode> findAllByOrderByCreatedAtDesc();

    @Modifying
    @Query("UPDATE PromoCode p SET p.currentUses = p.currentUses + 1, p.updatedAt = CURRENT_TIMESTAMP WHERE p.id = :id")
    void incrementUses(Long id);
}
