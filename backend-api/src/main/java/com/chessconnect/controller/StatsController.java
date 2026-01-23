package com.chessconnect.controller;

import com.chessconnect.dto.stats.AdvancedStatsResponse;
import com.chessconnect.security.UserDetailsImpl;
import com.chessconnect.service.AdvancedStatsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/stats")
public class StatsController {

    private final AdvancedStatsService advancedStatsService;

    public StatsController(AdvancedStatsService advancedStatsService) {
        this.advancedStatsService = advancedStatsService;
    }

    /**
     * Get advanced statistics for Premium students.
     * Returns detailed learning analytics.
     */
    @GetMapping("/advanced")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> getAdvancedStats(
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        try {
            AdvancedStatsResponse stats = advancedStatsService.getAdvancedStats(userDetails.getId());
            return ResponseEntity.ok(stats);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(403).body(Map.of(
                    "error", e.getMessage(),
                    "premiumRequired", true
            ));
        }
    }
}
