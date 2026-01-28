package com.chessconnect.exception;

/**
 * Exception thrown when an account is temporarily locked due to too many failed login attempts.
 */
public class AccountLockedException extends RuntimeException {

    private final long remainingLockoutSeconds;

    public AccountLockedException(String message, long remainingLockoutSeconds) {
        super(message);
        this.remainingLockoutSeconds = remainingLockoutSeconds;
    }

    public long getRemainingLockoutSeconds() {
        return remainingLockoutSeconds;
    }
}
