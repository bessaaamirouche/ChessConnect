package com.chessconnect.service;

import com.chessconnect.dto.auth.ForgotPasswordRequest;
import com.chessconnect.dto.auth.ResetPasswordRequest;
import com.chessconnect.model.PasswordResetToken;
import com.chessconnect.model.User;
import com.chessconnect.repository.PasswordResetTokenRepository;
import com.chessconnect.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    public PasswordResetService(
            PasswordResetTokenRepository tokenRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService
    ) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Transactional
    public void initiatePasswordReset(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.email()).orElse(null);

        if (user == null) {
            // Don't reveal that the email doesn't exist
            log.info("Password reset requested for non-existent email: {}", request.email());
            return;
        }

        // Create a new token
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        tokenRepository.save(token);

        // Send email
        String resetLink = frontendUrl + "/reset-password?token=" + token.getToken();
        emailService.sendPasswordResetEmail(user.getEmail(), user.getFirstName(), resetLink);

        log.info("Password reset email sent to: {}", user.getEmail());
    }

    public boolean validateToken(String token) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token).orElse(null);
        return resetToken != null && resetToken.isValid();
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = tokenRepository.findByToken(request.token())
                .orElseThrow(() -> new IllegalArgumentException("Token invalide ou expire"));

        if (!resetToken.isValid()) {
            throw new IllegalArgumentException("Token invalide ou expire");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        // Mark token as used
        resetToken.setUsedAt(LocalDateTime.now());
        tokenRepository.save(resetToken);

        log.info("Password reset successful for user: {}", user.getEmail());
    }

    // Clean up expired tokens every day at midnight
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        tokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
        log.info("Cleaned up expired password reset tokens");
    }
}
