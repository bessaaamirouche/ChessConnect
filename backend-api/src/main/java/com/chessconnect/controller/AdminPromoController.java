package com.chessconnect.controller;

import com.chessconnect.dto.promo.*;
import com.chessconnect.model.PromoCode;
import com.chessconnect.service.PromoCodeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/promo-codes")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPromoController {

    private final PromoCodeService promoCodeService;

    public AdminPromoController(PromoCodeService promoCodeService) {
        this.promoCodeService = promoCodeService;
    }

    @GetMapping
    public ResponseEntity<List<PromoCodeResponse>> getAllPromoCodes() {
        return ResponseEntity.ok(promoCodeService.getAllPromoCodes());
    }

    @PostMapping
    public ResponseEntity<PromoCodeResponse> createPromoCode(@Valid @RequestBody CreatePromoCodeRequest request) {
        PromoCode promo = promoCodeService.createPromoCode(request);
        return ResponseEntity.ok(promoCodeService.getPromoCodeById(promo.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PromoCodeResponse> getPromoCode(@PathVariable Long id) {
        return ResponseEntity.ok(promoCodeService.getPromoCodeById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PromoCodeResponse> updatePromoCode(@PathVariable Long id, @RequestBody UpdatePromoCodeRequest request) {
        PromoCode promo = promoCodeService.updatePromoCode(id, request);
        return ResponseEntity.ok(promoCodeService.getPromoCodeById(promo.getId()));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<Void> toggleActive(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        Boolean active = body.get("active");
        if (active == null) {
            return ResponseEntity.badRequest().build();
        }
        promoCodeService.toggleActive(id, active);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePromoCode(@PathVariable Long id) {
        promoCodeService.deletePromoCode(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/generate")
    public ResponseEntity<Map<String, String>> generateCode() {
        return ResponseEntity.ok(Map.of("code", promoCodeService.generateUniqueCode()));
    }

    @GetMapping("/{id}/usages")
    public ResponseEntity<List<PromoCodeUsageResponse>> getUsages(@PathVariable Long id) {
        return ResponseEntity.ok(promoCodeService.getUsagesByCodeId(id));
    }

    @GetMapping("/{id}/earnings")
    public ResponseEntity<List<ReferralEarningResponse>> getEarnings(@PathVariable Long id) {
        return ResponseEntity.ok(promoCodeService.getEarningsByCodeId(id));
    }

    @PatchMapping("/{id}/mark-paid")
    public ResponseEntity<Void> markEarningsAsPaid(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String ref = body.getOrDefault("paymentReference", "MANUAL-" + System.currentTimeMillis());
        promoCodeService.markEarningsAsPaid(id, ref);
        return ResponseEntity.ok().build();
    }
}
