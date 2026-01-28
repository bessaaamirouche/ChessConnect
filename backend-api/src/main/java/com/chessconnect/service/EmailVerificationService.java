package com.chessconnect.service;

import com.chessconnect.model.EmailVerificationToken;
import com.chessconnect.model.User;
import com.chessconnect.repository.EmailVerificationTokenRepository;
import com.chessconnect.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class EmailVerificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    public EmailVerificationService(
            EmailVerificationTokenRepository tokenRepository,
            UserRepository userRepository,
            EmailService emailService
    ) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    /**
     * Create verification token and send verification email to user.
     */
    @Transactional
    public void sendVerificationEmail(User user) {
        // Invalidate any existing tokens by not deleting but just letting them expire
        // Create new token
        EmailVerificationToken token = new EmailVerificationToken();
        token.setUser(user);
        tokenRepository.save(token);

        String verificationLink = frontendUrl + "/verify-email?token=" + token.getToken();

        log.info("Sending verification email to {} with link: {}", user.getEmail(), verificationLink);

        emailService.sendEmailVerificationEmail(
                user.getEmail(),
                user.getFirstName(),
                verificationLink
        );
    }

    /**
     * Verify user's email using the token.
     * @return true if verification successful, false otherwise
     */
    @Transactional
    public boolean verifyEmail(String tokenStr) {
        EmailVerificationToken token = tokenRepository.findByToken(tokenStr)
                .orElse(null);

        if (token == null) {
            log.warn("Verification token not found: {}", tokenStr);
            return false;
        }

        if (!token.isValid()) {
            log.warn("Verification token is invalid (expired or used): {}", tokenStr);
            return false;
        }

        // Mark token as used
        token.setUsedAt(LocalDateTime.now());
        tokenRepository.save(token);

        // Verify user's email
        User user = token.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        log.info("Email verified successfully for user: {}", user.getEmail());
        return true;
    }

    /**
     * Check if a token is valid (for validation endpoint).
     */
    public boolean isTokenValid(String tokenStr) {
        return tokenRepository.findByToken(tokenStr)
                .map(EmailVerificationToken::isValid)
                .orElse(false);
    }

    /**
     * Resend verification email if user hasn't verified yet.
     * Rate limited: can only resend once every 5 minutes.
     */
    @Transactional
    public ResendResult resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            // Don't reveal if email exists
            return new ResendResult(true, "Si cette adresse existe, un email de verification a ete envoye.");
        }

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            return new ResendResult(false, "Cet email est deja verifie.");
        }

        // Check for recent token (rate limiting - 5 minutes)
        var recentToken = tokenRepository.findFirstByUserIdAndUsedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
                user.getId(), LocalDateTime.now());

        if (recentToken.isPresent()) {
            LocalDateTime tokenCreatedAt = recentToken.get().getCreatedAt();
            if (tokenCreatedAt.isAfter(LocalDateTime.now().minusMinutes(5))) {
                return new ResendResult(false, "Veuillez attendre 5 minutes avant de demander un nouvel email.");
            }
        }

        sendVerificationEmail(user);
        return new ResendResult(true, "Un nouvel email de verification a ete envoye.");
    }

    /**
     * Clean up expired tokens (called by scheduled task).
     */
    @Transactional
    public void cleanupExpiredTokens() {
        tokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }

    public record ResendResult(boolean success, String message) {}
}
