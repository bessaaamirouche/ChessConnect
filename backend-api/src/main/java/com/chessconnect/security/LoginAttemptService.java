package com.chessconnect.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service to track and limit failed login attempts.
 * Implements account lockout after multiple failed attempts.
 */
@Service
public class LoginAttemptService {

    private static final Logger log = LoggerFactory.getLogger(LoginAttemptService.class);

    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCKOUT_DURATION_MINUTES = 15;
    private static final int BLOCK_DURATION_MINUTES = 30;

    // Track failed attempts by email
    private final Map<String, LoginAttemptInfo> attemptsByEmail = new ConcurrentHashMap<>();
    // Track failed attempts by IP
    private final Map<String, LoginAttemptInfo> attemptsByIp = new ConcurrentHashMap<>();

    /**
     * Record a failed login attempt
     */
    public void recordFailedAttempt(String email, String ipAddress) {
        recordAttempt(attemptsByEmail, normalizeEmail(email));
        recordAttempt(attemptsByIp, ipAddress);

        log.warn("Failed login attempt: email={}, ip={}, emailAttempts={}, ipAttempts={}",
                maskEmail(email), ipAddress,
                getAttemptCount(email, null),
                getAttemptCount(null, ipAddress));
    }

    /**
     * Record a successful login (resets the attempt counter)
     */
    public void recordSuccessfulLogin(String email, String ipAddress) {
        attemptsByEmail.remove(normalizeEmail(email));
        attemptsByIp.remove(ipAddress);
        log.info("Successful login: email={}, ip={}", maskEmail(email), ipAddress);
    }

    /**
     * Check if login is blocked for given email or IP
     */
    public boolean isBlocked(String email, String ipAddress) {
        return isBlockedByEmail(email) || isBlockedByIp(ipAddress);
    }

    /**
     * Check if email is blocked
     */
    public boolean isBlockedByEmail(String email) {
        return isBlocked(attemptsByEmail, normalizeEmail(email));
    }

    /**
     * Check if IP is blocked
     */
    public boolean isBlockedByIp(String ipAddress) {
        return isBlocked(attemptsByIp, ipAddress);
    }

    /**
     * Get remaining lockout time in seconds
     */
    public long getRemainingLockoutSeconds(String email, String ipAddress) {
        long emailLockout = getRemainingSeconds(attemptsByEmail, normalizeEmail(email));
        long ipLockout = getRemainingSeconds(attemptsByIp, ipAddress);
        return Math.max(emailLockout, ipLockout);
    }

    /**
     * Get the number of failed attempts
     */
    public int getAttemptCount(String email, String ipAddress) {
        int emailAttempts = email != null ? getAttempts(attemptsByEmail, normalizeEmail(email)) : 0;
        int ipAttempts = ipAddress != null ? getAttempts(attemptsByIp, ipAddress) : 0;
        return Math.max(emailAttempts, ipAttempts);
    }

    private void recordAttempt(Map<String, LoginAttemptInfo> attempts, String key) {
        if (key == null || key.isBlank()) return;

        attempts.compute(key, (k, info) -> {
            if (info == null) {
                return new LoginAttemptInfo(1, LocalDateTime.now(), null);
            }

            // If lockout has expired, reset counter
            if (info.lockoutUntil != null && LocalDateTime.now().isAfter(info.lockoutUntil)) {
                return new LoginAttemptInfo(1, LocalDateTime.now(), null);
            }

            int newCount = info.attemptCount + 1;
            LocalDateTime lockoutUntil = null;

            if (newCount >= MAX_ATTEMPTS) {
                // Calculate lockout duration (increases with repeated lockouts)
                int multiplier = (newCount / MAX_ATTEMPTS);
                int lockoutMinutes = Math.min(LOCKOUT_DURATION_MINUTES * multiplier, BLOCK_DURATION_MINUTES);
                lockoutUntil = LocalDateTime.now().plusMinutes(lockoutMinutes);
                log.warn("Account locked: key={}, attempts={}, lockoutUntil={}",
                        key.contains("@") ? maskEmail(key) : key, newCount, lockoutUntil);
            }

            return new LoginAttemptInfo(newCount, info.firstAttempt, lockoutUntil);
        });
    }

    private boolean isBlocked(Map<String, LoginAttemptInfo> attempts, String key) {
        if (key == null || key.isBlank()) return false;

        LoginAttemptInfo info = attempts.get(key);
        if (info == null) return false;

        if (info.lockoutUntil != null && LocalDateTime.now().isBefore(info.lockoutUntil)) {
            return true;
        }

        return false;
    }

    private long getRemainingSeconds(Map<String, LoginAttemptInfo> attempts, String key) {
        if (key == null || key.isBlank()) return 0;

        LoginAttemptInfo info = attempts.get(key);
        if (info == null || info.lockoutUntil == null) return 0;

        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(info.lockoutUntil)) return 0;

        return java.time.Duration.between(now, info.lockoutUntil).getSeconds();
    }

    private int getAttempts(Map<String, LoginAttemptInfo> attempts, String key) {
        if (key == null || key.isBlank()) return 0;
        LoginAttemptInfo info = attempts.get(key);
        return info != null ? info.attemptCount : 0;
    }

    private String normalizeEmail(String email) {
        return email != null ? email.toLowerCase().trim() : null;
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        int atIndex = email.indexOf('@');
        if (atIndex <= 2) return "***" + email.substring(atIndex);
        return email.substring(0, 2) + "***" + email.substring(atIndex);
    }

    /**
     * Cleanup old entries every 5 minutes
     */
    @Scheduled(fixedRate = 300000)
    public void cleanupOldEntries() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(1);
        int emailBefore = attemptsByEmail.size();
        int ipBefore = attemptsByIp.size();

        attemptsByEmail.entrySet().removeIf(e ->
            e.getValue().firstAttempt.isBefore(cutoff) &&
            (e.getValue().lockoutUntil == null || LocalDateTime.now().isAfter(e.getValue().lockoutUntil)));

        attemptsByIp.entrySet().removeIf(e ->
            e.getValue().firstAttempt.isBefore(cutoff) &&
            (e.getValue().lockoutUntil == null || LocalDateTime.now().isAfter(e.getValue().lockoutUntil)));

        int emailRemoved = emailBefore - attemptsByEmail.size();
        int ipRemoved = ipBefore - attemptsByIp.size();

        if (emailRemoved > 0 || ipRemoved > 0) {
            log.debug("Login attempt cleanup: removed {} email entries, {} IP entries", emailRemoved, ipRemoved);
        }
    }

    private record LoginAttemptInfo(int attemptCount, LocalDateTime firstAttempt, LocalDateTime lockoutUntil) {}
}
