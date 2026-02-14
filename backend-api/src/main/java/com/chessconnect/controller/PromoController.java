package com.chessconnect.controller;

import com.chessconnect.dto.promo.ValidatePromoCodeResponse;
import com.chessconnect.security.UserDetailsImpl;
import com.chessconnect.service.PromoCodeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/promo")
public class PromoController {

    private final PromoCodeService promoCodeService;

    public PromoController(PromoCodeService promoCodeService) {
        this.promoCodeService = promoCodeService;
    }

    @GetMapping("/validate")
    public ResponseEntity<ValidatePromoCodeResponse> validateCode(
            @RequestParam String code,
            @RequestParam int amount,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        ValidatePromoCodeResponse response = promoCodeService.validateCode(code, userDetails.getId(), amount);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/apply-referral")
    public ResponseEntity<Map<String, String>> applyReferral(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        String code = body.get("code");
        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Code requis"));
        }
        promoCodeService.applyReferralAtSignup(userDetails.getId(), code);
        return ResponseEntity.ok(Map.of("message", "Code parrainage applique"));
    }
}
