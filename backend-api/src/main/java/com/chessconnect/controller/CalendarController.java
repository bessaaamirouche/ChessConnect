package com.chessconnect.controller;

import com.chessconnect.model.User;
import com.chessconnect.repository.UserRepository;
import com.chessconnect.service.GoogleCalendarService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/calendar")
public class CalendarController {

    private final GoogleCalendarService googleCalendarService;
    private final UserRepository userRepository;

    public CalendarController(GoogleCalendarService googleCalendarService, UserRepository userRepository) {
        this.googleCalendarService = googleCalendarService;
        this.userRepository = userRepository;
    }

    /**
     * Get Google Calendar OAuth authorization URL
     */
    @GetMapping("/google/auth-url")
    public ResponseEntity<?> getGoogleAuthUrl(@AuthenticationPrincipal UserDetails userDetails) {
        if (!googleCalendarService.isConfigured()) {
            return ResponseEntity.ok(Map.of(
                    "configured", false,
                    "message", "Google Calendar n'est pas configure sur ce serveur"
            ));
        }

        String authUrl = googleCalendarService.getAuthorizationUrl();
        return ResponseEntity.ok(Map.of(
                "configured", true,
                "authUrl", authUrl
        ));
    }

    /**
     * Handle Google OAuth callback
     */
    @PostMapping("/google/callback")
    public ResponseEntity<?> handleGoogleCallback(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> request) {

        String code = request.get("code");
        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Code d'autorisation manquant"));
        }

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        try {
            googleCalendarService.handleCallback(code, user);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Google Calendar connecte avec succes"
            ));
        } catch (IOException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Erreur lors de la connexion a Google Calendar"));
        }
    }

    /**
     * Disconnect Google Calendar
     */
    @DeleteMapping("/google/disconnect")
    public ResponseEntity<?> disconnectGoogle(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        googleCalendarService.disconnect(user);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Google Calendar deconnecte"
        ));
    }

    /**
     * Get Google Calendar connection status
     */
    @GetMapping("/google/status")
    public ResponseEntity<?> getGoogleStatus(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(Map.of(
                "configured", googleCalendarService.isConfigured(),
                "connected", googleCalendarService.isConnected(user),
                "enabled", Boolean.TRUE.equals(user.getGoogleCalendarEnabled())
        ));
    }
}
