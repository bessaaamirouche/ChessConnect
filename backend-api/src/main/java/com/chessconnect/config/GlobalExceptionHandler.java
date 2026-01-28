package com.chessconnect.config;

import com.chessconnect.exception.AccountLockedException;
import com.chessconnect.exception.AccountSuspendedException;
import com.chessconnect.exception.EmailNotVerifiedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<Map<String, Object>> handleAccountLocked(AccountLockedException ex) {
        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Map.of(
                        "error", ex.getMessage(),
                        "code", "ACCOUNT_LOCKED",
                        "retryAfter", ex.getRemainingLockoutSeconds()
                ));
    }

    @ExceptionHandler(AccountSuspendedException.class)
    public ResponseEntity<Map<String, Object>> handleAccountSuspended(AccountSuspendedException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(Map.of(
                        "error", ex.getMessage(),
                        "code", "ACCOUNT_SUSPENDED"
                ));
    }

    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<Map<String, Object>> handleEmailNotVerified(EmailNotVerifiedException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(Map.of(
                        "error", ex.getMessage(),
                        "code", "EMAIL_NOT_VERIFIED",
                        "email", ex.getEmail()
                ));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Email ou mot de passe incorrect"));
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleUsernameNotFound(UsernameNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Email ou mot de passe incorrect"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationException(MethodArgumentNotValidException ex) {
        // Get the first field error with its message
        var fieldError = ex.getBindingResult().getFieldErrors().stream().findFirst().orElse(null);
        String fieldName = fieldError != null ? fieldError.getField() : "";
        String defaultMessage = fieldError != null ? fieldError.getDefaultMessage() : "Donnees invalides";

        String message = switch (fieldName) {
            case "scheduledAt" -> "L'heure du cours doit etre dans le futur";
            case "email" -> "Email invalide";
            case "password" -> "Le mot de passe doit contenir au moins 8 caracteres, une majuscule, une minuscule, un chiffre et un caractere special";
            case "firstName" -> "Le prenom doit contenir entre 1 et 100 caracteres (lettres uniquement)";
            case "lastName" -> "Le nom doit contenir entre 1 et 100 caracteres (lettres uniquement)";
            case "hourlyRateCents" -> "Le tarif horaire doit etre entre 10 et 500 euros";
            case "eloRating" -> "Le classement ELO doit etre entre 0 et 3500";
            case "bio" -> "La bio ne doit pas depasser 2000 caracteres";
            default -> defaultMessage != null ? defaultMessage : "Donnees invalides";
        };

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", message));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }
}
