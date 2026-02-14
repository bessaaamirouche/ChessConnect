package com.chessconnect.repository;

import com.chessconnect.model.ReferralEarning;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReferralEarningRepository extends JpaRepository<ReferralEarning, Long> {

    List<ReferralEarning> findByPromoCodeIdOrderByCreatedAtDesc(Long promoCodeId);

    @Query("SELECT COALESCE(SUM(e.referrerEarningCents), 0) FROM ReferralEarning e WHERE e.promoCode.id = :promoCodeId")
    long getTotalEarningsByCodeId(Long promoCodeId);

    @Query("SELECT COALESCE(SUM(e.referrerEarningCents), 0) FROM ReferralEarning e WHERE e.promoCode.id = :promoCodeId AND e.isPaid = false")
    long getUnpaidEarningsByCodeId(Long promoCodeId);

    List<ReferralEarning> findByPromoCodeIdAndIsPaidFalse(Long promoCodeId);
}
