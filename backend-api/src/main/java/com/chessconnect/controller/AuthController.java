package com.chessconnect.controller;

import com.chessconnect.dto.auth.AdminLoginRequest;
import com.chessconnect.dto.auth.AuthResponse;
import com.chessconnect.dto.auth.ForgotPasswordRequest;
import com.chessconnect.dto.auth.LoginRequest;
import com.chessconnect.dto.auth.RegisterRequest;
import com.chessconnect.dto.auth.RegisterResponse;
import com.chessconnect.dto.auth.ResetPasswordRequest;
import com.chessconnect.security.JwtAuthenticationFilter;
import com.chessconnect.service.AuthService;
import com.chessconnect.service.EmailVerificationService;
import com.chessconnect.service.PasswordResetService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
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
    private final EmailVerificationService emailVerificationService;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    public AuthController(
            AuthService authService,
            PasswordResetService passwordResetService,
            EmailVerificationService emailVerificationService
    ) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
        this.emailVerificationService = emailVerificationService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse registerResponse = authService.register(request);
        // No auth cookie set - user needs to verify email first
        return ResponseEntity.ok(registerResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {
        String clientIp = getClientIp(httpRequest);
        var loginResult = authService.login(request, clientIp);
        setAuthCookie(response, loginResult.token());
        return ResponseEntity.ok(loginResult.response());
    }

    @PostMapping("/admin-login")
    public ResponseEntity<AuthResponse> adminLogin(@Valid @RequestBody AdminLoginRequest request, HttpServletResponse response) {
        var loginResult = authService.adminLogin(request);
        setAuthCookie(response, loginResult.token());
        return ResponseEntity.ok(loginResult.response());
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
        // Use Lax to allow cookie on redirects from external sites (Stripe checkout)
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }

    /**
     * Get client IP address for login attempt tracking.
     */
    private String getClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        // Only trust proxy headers from localhost/Docker
        if (isTrustedProxy(remoteAddr)) {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                return xForwardedFor.split(",")[0].trim();
            }
            String xRealIp = request.getHeader("X-Real-IP");
            if (xRealIp != null && !xRealIp.isEmpty()) {
                return xRealIp;
            }
        }
        return remoteAddr;
    }

    private boolean isTrustedProxy(String ip) {
        if (ip == null) return false;
        if (ip.equals("127.0.0.1") || ip.equals("::1")) return true;
        if (ip.startsWith("10.") || ip.startsWith("192.168.")) return true;
        // RFC 1918: 172.16.0.0/12 = 172.16.x.x through 172.31.x.x only
        if (ip.startsWith("172.")) {
            try {
                int second = Integer.parseInt(ip.split("\\.")[1]);
                return second >= 16 && second <= 31;
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                return false;
            }
        }
        return false;
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

    @GetMapping("/verify-email")
    public ResponseEntity<Map<String, Object>> verifyEmail(@RequestParam String token) {
        boolean success = emailVerificationService.verifyEmail(token);
        if (success) {
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Votre email a ete verifie avec succes. Vous pouvez maintenant vous connecter."
            ));
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Le lien de verification est invalide ou a expire."
            ));
        }
    }

    @GetMapping("/verify-email/validate")
    public ResponseEntity<Map<String, Boolean>> validateVerificationToken(@RequestParam String token) {
        boolean isValid = emailVerificationService.isTokenValid(token);
        return ResponseEntity.ok(Map.of("valid", isValid));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Map<String, Object>> resendVerificationEmail(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Email requis"
            ));
        }

        var result = emailVerificationService.resendVerificationEmail(email);
        return ResponseEntity.ok(Map.of(
            "success", result.success(),
            "message", result.message()
        ));
    }
}
