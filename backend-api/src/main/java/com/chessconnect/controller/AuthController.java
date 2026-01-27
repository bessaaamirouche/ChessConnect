package com.chessconnect.controller;

import com.chessconnect.dto.auth.AdminLoginRequest;
import com.chessconnect.dto.auth.AuthResponse;
import com.chessconnect.dto.auth.ForgotPasswordRequest;
import com.chessconnect.dto.auth.LoginRequest;
import com.chessconnect.dto.auth.RegisterRequest;
import com.chessconnect.dto.auth.ResetPasswordRequest;
import com.chessconnect.security.JwtAuthenticationFilter;
import com.chessconnect.service.AuthService;
import com.chessconnect.service.PasswordResetService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    public AuthController(AuthService authService, PasswordResetService passwordResetService) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletResponse response) {
        AuthResponse authResponse = authService.register(request);
        setAuthCookie(response, authResponse.token());
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        AuthResponse authResponse = authService.login(request);
        setAuthCookie(response, authResponse.token());
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/admin-login")
    public ResponseEntity<AuthResponse> adminLogin(@Valid @RequestBody AdminLoginRequest request, HttpServletResponse response) {
        AuthResponse authResponse = authService.adminLogin(request);
        setAuthCookie(response, authResponse.token());
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletResponse response) {
        // Clear the auth cookie
        Cookie cookie = new Cookie(JwtAuthenticationFilter.JWT_COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        return ResponseEntity.ok(Map.of("message", "Deconnexion reussie"));
    }

    private void setAuthCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie(JwtAuthenticationFilter.JWT_COOKIE_NAME, token);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge((int) (jwtExpiration / 1000)); // Convert ms to seconds
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.initiatePasswordReset(request);
        // Always return success to prevent email enumeration
        return ResponseEntity.ok(Map.of(
            "message", "Si cette adresse email existe, un lien de reinitialisation a ete envoye."
        ));
    }

    @GetMapping("/reset-password/validate")
    public ResponseEntity<Map<String, Boolean>> validateResetToken(@RequestParam String token) {
        boolean isValid = passwordResetService.validateToken(token);
        return ResponseEntity.ok(Map.of("valid", isValid));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request);
        return ResponseEntity.ok(Map.of(
            "message", "Votre mot de passe a ete reinitialise avec succes."
        ));
    }
}
