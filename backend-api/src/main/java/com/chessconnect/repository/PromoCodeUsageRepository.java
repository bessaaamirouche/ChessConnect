package com.chessconnect.repository;

import com.chessconnect.model.PromoCodeUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PromoCodeUsageRepository extends JpaRepository<PromoCodeUsage, Long> {

    List<PromoCodeUsage> findByPromoCodeIdOrderByUsedAtDesc(Long promoCodeId);

    boolean existsByPromoCodeIdAndUserId(Long promoCodeId, Long userId);

    @Query("SELECT COALESCE(SUM(u.discountAmountCents), 0) FROM PromoCodeUsage u WHERE u.promoCode.id = :promoCodeId")
    long getTotalDiscountByCodeId(Long promoCodeId);
}
