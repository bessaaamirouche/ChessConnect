package com.chessconnect.controller;

import com.chessconnect.dto.tracking.PageViewRequest;
import com.chessconnect.security.UserDetailsImpl;
import com.chessconnect.service.AnalyticsService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tracking")
public class TrackingController {

    private final AnalyticsService analyticsService;

    public TrackingController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /**
     * Track a page view.
     * This endpoint is public to track both authenticated and anonymous users.
     */
    @PostMapping("/pageview")
    public ResponseEntity<Void> trackPageView(
            @Valid @RequestBody PageViewRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        Long userId = userDetails != null ? userDetails.getId() : null;
        analyticsService.trackPageView(userId, request.pageUrl(), request.sessionId());
        return ResponseEntity.ok().build();
    }
}
