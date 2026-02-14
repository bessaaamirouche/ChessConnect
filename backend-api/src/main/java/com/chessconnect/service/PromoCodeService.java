package com.chessconnect.service;

import com.chessconnect.dto.promo.*;
import com.chessconnect.model.*;
import com.chessconnect.model.enums.DiscountType;
import com.chessconnect.model.enums.PromoCodeType;
import com.chessconnect.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PromoCodeService {

    private static final Logger log = LoggerFactory.getLogger(PromoCodeService.class);
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PromoCodeRepository promoCodeRepository;
    private final PromoCodeUsageRepository promoCodeUsageRepository;
    private final ReferralEarningRepository referralEarningRepository;
    private final UserRepository userRepository;
    private final LessonRepository lessonRepository;
    private final PaymentRepository paymentRepository;

    public PromoCodeService(
            PromoCodeRepository promoCodeRepository,
            PromoCodeUsageRepository promoCodeUsageRepository,
            ReferralEarningRepository referralEarningRepository,
            UserRepository userRepository,
            LessonRepository lessonRepository,
            PaymentRepository paymentRepository
    ) {
        this.promoCodeRepository = promoCodeRepository;
        this.promoCodeUsageRepository = promoCodeUsageRepository;
        this.referralEarningRepository = referralEarningRepository;
        this.userRepository = userRepository;
        this.lessonRepository = lessonRepository;
        this.paymentRepository = paymentRepository;
    }

    // ==================== CRUD ====================

    @Transactional
    public PromoCode createPromoCode(CreatePromoCodeRequest request) {
        if (promoCodeRepository.existsByCodeIgnoreCase(request.code())) {
            throw new IllegalArgumentException("Ce code existe deja");
        }

        PromoCode promo = new PromoCode();
        promo.setCode(request.code().toUpperCase().trim());
        promo.setCodeType(request.codeType());
        promo.setDiscountType(request.discountType());
        promo.setDiscountPercent(request.discountPercent());
        promo.setReferrerName(request.referrerName());
        promo.setReferrerEmail(request.referrerEmail());
        promo.setPremiumDays(request.premiumDays() != null ? request.premiumDays() : 0);
        promo.setRevenueSharePercent(request.revenueSharePercent() != null ? request.revenueSharePercent() : 0.0);
        promo.setMaxUses(request.maxUses());
        promo.setFirstLessonOnly(request.firstLessonOnly() != null ? request.firstLessonOnly() : false);
        promo.setMinAmountCents(request.minAmountCents());
        promo.setExpiresAt(request.expiresAt());

        promo = promoCodeRepository.save(promo);
        log.info("Created promo code: {} (type={})", promo.getCode(), promo.getCodeType());
        return promo;
    }

    @Transactional
    public PromoCode updatePromoCode(Long id, UpdatePromoCodeRequest request) {
        PromoCode promo = promoCodeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Code promo introuvable"));

        if (request.discountType() != null) promo.setDiscountType(request.discountType());
        if (request.discountPercent() != null) promo.setDiscountPercent(request.discountPercent());
        if (request.referrerName() != null) promo.setReferrerName(request.referrerName());
        if (request.referrerEmail() != null) promo.setReferrerEmail(request.referrerEmail());
        if (request.premiumDays() != null) promo.setPremiumDays(request.premiumDays());
        if (request.revenueSharePercent() != null) promo.setRevenueSharePercent(request.revenueSharePercent());
        if (request.maxUses() != null) promo.setMaxUses(request.maxUses());
        if (request.firstLessonOnly() != null) promo.setFirstLessonOnly(request.firstLessonOnly());
        if (request.minAmountCents() != null) promo.setMinAmountCents(request.minAmountCents());
        if (request.expiresAt() != null) promo.setExpiresAt(request.expiresAt());

        promo = promoCodeRepository.save(promo);
        log.info("Updated promo code: {}", promo.getCode());
        return promo;
    }

    @Transactional
    public void toggleActive(Long id, boolean active) {
        PromoCode promo = promoCodeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Code promo introuvable"));
        promo.setIsActive(active);
        promoCodeRepository.save(promo);
        log.info("Toggled promo code {} active={}", promo.getCode(), active);
    }

    @Transactional
    public void deletePromoCode(Long id) {
        PromoCode promo = promoCodeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Code promo introuvable"));
        promoCodeRepository.delete(promo);
        log.info("Deleted promo code: {}", promo.getCode());
    }

    // ==================== QUERIES ====================

    public List<PromoCodeResponse> getAllPromoCodes() {
        return promoCodeRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    public PromoCodeResponse getPromoCodeById(Long id) {
        PromoCode promo = promoCodeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Code promo introuvable"));
        return toResponse(promo);
    }

    public String generateUniqueCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder("MC-");
            for (int i = 0; i < 8; i++) {
                sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
            }
            code = sb.toString();
        } while (promoCodeRepository.existsByCodeIgnoreCase(code));
        return code;
    }

    // ==================== VALIDATION ====================

    @Transactional(readOnly = true)
    public ValidatePromoCodeResponse validateCode(String code, Long userId, int lessonAmountCents) {
        PromoCode promo = promoCodeRepository.findByCodeIgnoreCase(code).orElse(null);

        if (promo == null) {
            return new ValidatePromoCodeResponse(false, "Code invalide", null, null, lessonAmountCents, 0);
        }
        if (!promo.getIsActive()) {
            return new ValidatePromoCodeResponse(false, "Ce code n'est plus actif", null, null, lessonAmountCents, 0);
        }
        if (promo.isExpired()) {
            return new ValidatePromoCodeResponse(false, "Ce code a expire", null, null, lessonAmountCents, 0);
        }
        if (promo.hasReachedMaxUses()) {
            return new ValidatePromoCodeResponse(false, "Ce code a atteint son nombre maximum d'utilisations", null, null, lessonAmountCents, 0);
        }
        if (promoCodeUsageRepository.existsByPromoCodeIdAndUserId(promo.getId(), userId)) {
            return new ValidatePromoCodeResponse(false, "Vous avez deja utilise ce code", null, null, lessonAmountCents, 0);
        }
        if (promo.getMinAmountCents() != null && lessonAmountCents < promo.getMinAmountCents()) {
            return new ValidatePromoCodeResponse(false,
                    String.format("Montant minimum requis : %.2f EUR", promo.getMinAmountCents() / 100.0),
                    null, null, lessonAmountCents, 0);
        }
        if (Boolean.TRUE.equals(promo.getFirstLessonOnly())) {
            long lessonCount = paymentRepository.countByPayerIdAndStatus(userId, com.chessconnect.model.enums.PaymentStatus.COMPLETED);
            if (lessonCount > 0) {
                return new ValidatePromoCodeResponse(false, "Ce code est valable uniquement pour le premier cours", null, null, lessonAmountCents, 0);
            }
        }

        // Calculate discount
        if (promo.getCodeType() == PromoCodeType.PROMO) {
            if (promo.getDiscountType() == DiscountType.STUDENT_DISCOUNT) {
                int discountCents = (int) (lessonAmountCents * promo.getDiscountPercent() / 100.0);
                int finalPrice = lessonAmountCents - discountCents;
                return new ValidatePromoCodeResponse(true,
                        String.format("Reduction de %.0f%% appliquee", promo.getDiscountPercent()),
                        DiscountType.STUDENT_DISCOUNT, promo.getDiscountPercent(), finalPrice, discountCents);
            } else if (promo.getDiscountType() == DiscountType.COMMISSION_REDUCTION) {
                return new ValidatePromoCodeResponse(true,
                        String.format("Commission plateforme reduite de %.0f%%", promo.getDiscountPercent()),
                        DiscountType.COMMISSION_REDUCTION, promo.getDiscountPercent(), lessonAmountCents, 0);
            }
        } else if (promo.getCodeType() == PromoCodeType.REFERRAL) {
            return new ValidatePromoCodeResponse(true,
                    "Code parrainage valide", null, null, lessonAmountCents, 0);
        }

        return new ValidatePromoCodeResponse(false, "Type de code non supporte", null, null, lessonAmountCents, 0);
    }

    // ==================== APPLICATION ====================

    @Transactional
    public PromoCodeUsage applyPromoCode(String code, Long userId, Long lessonId, Long paymentId, int originalAmountCents) {
        PromoCode promo = promoCodeRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new IllegalArgumentException("Code promo introuvable"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Lesson lesson = lessonId != null ? lessonRepository.findById(lessonId).orElse(null) : null;
        Payment payment = paymentId != null ? paymentRepository.findById(paymentId).orElse(null) : null;

        int discountAmountCents = 0;
        int commissionSavedCents = 0;

        if (promo.getCodeType() == PromoCodeType.PROMO) {
            if (promo.getDiscountType() == DiscountType.STUDENT_DISCOUNT) {
                discountAmountCents = (int) (originalAmountCents * promo.getDiscountPercent() / 100.0);
            } else if (promo.getDiscountType() == DiscountType.COMMISSION_REDUCTION) {
                int standardCommission = (originalAmountCents * 10) / 100;
                int reducedCommission = (int) (standardCommission * (100.0 - promo.getDiscountPercent()) / 100.0);
                commissionSavedCents = standardCommission - reducedCommission;
            }
        }

        PromoCodeUsage usage = new PromoCodeUsage();
        usage.setPromoCode(promo);
        usage.setUser(user);
        usage.setLesson(lesson);
        usage.setPayment(payment);
        usage.setOriginalAmountCents(originalAmountCents);
        usage.setDiscountAmountCents(discountAmountCents);
        usage.setCommissionSavedCents(commissionSavedCents);

        usage = promoCodeUsageRepository.save(usage);
        promoCodeRepository.incrementUses(promo.getId());

        log.info("Applied promo code {} for user {} (discount={}c, commissionSaved={}c)",
                promo.getCode(), userId, discountAmountCents, commissionSavedCents);
        return usage;
    }

    @Transactional
    public void applyReferralAtSignup(Long userId, String referralCode) {
        PromoCode promo = promoCodeRepository.findByCodeIgnoreCase(referralCode).orElse(null);
        if (promo == null || promo.getCodeType() != PromoCodeType.REFERRAL || !promo.getIsActive()) {
            log.warn("Invalid referral code at signup: {}", referralCode);
            return;
        }
        if (promo.isExpired() || promo.hasReachedMaxUses()) {
            log.warn("Expired or maxed-out referral code at signup: {}", referralCode);
            return;
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        user.setReferredByCode(promo);
        user.setReferralCodeUsedAt(LocalDateTime.now());

        // Grant premium trial days
        if (promo.getPremiumDays() != null && promo.getPremiumDays() > 0) {
            LocalDate currentEnd = user.getPremiumTrialEnd();
            LocalDate base = (currentEnd != null && currentEnd.isAfter(LocalDate.now())) ? currentEnd : LocalDate.now();
            user.setPremiumTrialEnd(base.plusDays(promo.getPremiumDays()));
            log.info("Extended premium trial for user {} by {} days (referral {})",
                    userId, promo.getPremiumDays(), promo.getCode());
        }

        userRepository.save(user);
        promoCodeRepository.incrementUses(promo.getId());
        log.info("Applied referral code {} at signup for user {}", promo.getCode(), userId);
    }

    @Transactional
    public void recordReferralEarning(Long studentId, Long lessonId, int lessonAmountCents, int platformCommissionCents) {
        User student = userRepository.findById(studentId).orElse(null);
        if (student == null || student.getReferredByCode() == null) {
            return;
        }

        PromoCode referralCode = student.getReferredByCode();
        if (referralCode.getRevenueSharePercent() == null || referralCode.getRevenueSharePercent() <= 0) {
            return;
        }

        int earningCents = (int) (platformCommissionCents * referralCode.getRevenueSharePercent() / 100.0);
        if (earningCents <= 0) return;

        Lesson lesson = lessonId != null ? lessonRepository.findById(lessonId).orElse(null) : null;

        ReferralEarning earning = new ReferralEarning();
        earning.setPromoCode(referralCode);
        earning.setReferredUser(student);
        earning.setLesson(lesson);
        earning.setLessonAmountCents(lessonAmountCents);
        earning.setPlatformCommissionCents(platformCommissionCents);
        earning.setReferrerEarningCents(earningCents);

        referralEarningRepository.save(earning);
        log.info("Recorded referral earning: {}c for code {} (student={}, lesson={})",
                earningCents, referralCode.getCode(), studentId, lessonId);
    }

    // ==================== ADMIN: USAGES & EARNINGS ====================

    public List<PromoCodeUsageResponse> getUsagesByCodeId(Long promoCodeId) {
        return promoCodeUsageRepository.findByPromoCodeIdOrderByUsedAtDesc(promoCodeId).stream()
                .map(u -> new PromoCodeUsageResponse(
                        u.getId(),
                        u.getUser().getId(),
                        u.getUser().getFullName(),
                        u.getLesson() != null ? u.getLesson().getId() : null,
                        u.getOriginalAmountCents(),
                        u.getDiscountAmountCents(),
                        u.getCommissionSavedCents(),
                        u.getUsedAt()
                ))
                .toList();
    }

    public List<ReferralEarningResponse> getEarningsByCodeId(Long promoCodeId) {
        return referralEarningRepository.findByPromoCodeIdOrderByCreatedAtDesc(promoCodeId).stream()
                .map(e -> new ReferralEarningResponse(
                        e.getId(),
                        e.getReferredUser().getId(),
                        e.getReferredUser().getFullName(),
                        e.getLesson() != null ? e.getLesson().getId() : null,
                        e.getLessonAmountCents(),
                        e.getPlatformCommissionCents(),
                        e.getReferrerEarningCents(),
                        e.getIsPaid(),
                        e.getPaidAt(),
                        e.getPaymentReference(),
                        e.getCreatedAt()
                ))
                .toList();
    }

    @Transactional
    public void markEarningsAsPaid(Long promoCodeId, String paymentReference) {
        List<ReferralEarning> unpaid = referralEarningRepository.findByPromoCodeIdAndIsPaidFalse(promoCodeId);
        LocalDateTime now = LocalDateTime.now();
        for (ReferralEarning earning : unpaid) {
            earning.setIsPaid(true);
            earning.setPaidAt(now);
            earning.setPaymentReference(paymentReference);
        }
        referralEarningRepository.saveAll(unpaid);
        log.info("Marked {} earnings as paid for code ID {} (ref={})", unpaid.size(), promoCodeId, paymentReference);
    }

    // ==================== HELPERS ====================

    private PromoCodeResponse toResponse(PromoCode promo) {
        long totalDiscount = promoCodeUsageRepository.getTotalDiscountByCodeId(promo.getId());
        long totalEarnings = referralEarningRepository.getTotalEarningsByCodeId(promo.getId());
        long unpaidEarnings = referralEarningRepository.getUnpaidEarningsByCodeId(promo.getId());

        return new PromoCodeResponse(
                promo.getId(),
                promo.getCode(),
                promo.getCodeType(),
                promo.getDiscountType(),
                promo.getDiscountPercent(),
                promo.getReferrerName(),
                promo.getReferrerEmail(),
                promo.getPremiumDays(),
                promo.getRevenueSharePercent(),
                promo.getMaxUses(),
                promo.getCurrentUses(),
                promo.getFirstLessonOnly(),
                promo.getMinAmountCents(),
                promo.getIsActive(),
                promo.getExpiresAt(),
                promo.getCreatedAt(),
                totalDiscount,
                totalEarnings,
                unpaidEarnings
        );
    }
}
