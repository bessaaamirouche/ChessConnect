package com.chessconnect.service;

import com.chessconnect.dto.auth.AdminLoginRequest;
import com.chessconnect.dto.auth.AuthResponse;
import com.chessconnect.dto.auth.LoginRequest;
import com.chessconnect.dto.auth.RegisterRequest;
import com.chessconnect.dto.auth.RegisterResponse;
import com.chessconnect.exception.AccountLockedException;
import com.chessconnect.exception.AccountSuspendedException;
import com.chessconnect.exception.EmailNotVerifiedException;
import com.chessconnect.model.Progress;
import com.chessconnect.model.User;
import com.chessconnect.model.enums.ChessLevel;
import com.chessconnect.model.enums.UserRole;
import com.chessconnect.repository.ProgressRepository;
import com.chessconnect.repository.UserRepository;
import com.chessconnect.security.JwtService;
import com.chessconnect.security.LoginAttemptService;
import com.chessconnect.security.UserDetailsImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final ProgressRepository progressRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailVerificationService emailVerificationService;
    private final LoginAttemptService loginAttemptService;

    public AuthService(
            UserRepository userRepository,
            ProgressRepository progressRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager,
            EmailVerificationService emailVerificationService,
            LoginAttemptService loginAttemptService
    ) {
        this.userRepository = userRepository;
        this.progressRepository = progressRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.emailVerificationService = emailVerificationService;
        this.loginAttemptService = loginAttemptService;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Cet email est deja utilise");
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setRole(request.role());
        user.setEmailVerified(false); // Require email verification

        if (request.role() == UserRole.TEACHER) {
            // Validate required bio for teachers
            if (request.bio() == null || request.bio().trim().length() < 20) {
                throw new IllegalArgumentException("La biographie est obligatoire (minimum 20 caracteres)");
            }
            user.setHourlyRateCents(request.hourlyRateCents() != null ? request.hourlyRateCents() : 5000);
            user.setBio(request.bio().trim());
            // Save languages for teachers
            if (request.languages() != null && !request.languages().isEmpty()) {
                user.setLanguages(String.join(",", request.languages()));
            } else {
                user.setLanguages("FR"); // Default to French
            }
        } else if (request.role() == UserRole.STUDENT) {
            user.setBirthDate(request.birthDate());
            user.setEloRating(request.eloRating());
            // Set starting course (default to 1 if not specified)
            user.setCurrentCourseId(request.startingCourseId() != null ? request.startingCourseId() : 1);
        }

        User savedUser = userRepository.save(user);

        if (request.role() == UserRole.STUDENT) {
            Progress progress = new Progress();
            progress.setStudent(savedUser);
            progress.setCurrentLevel(ChessLevel.A);
            progressRepository.save(progress);
        }

        // Send verification email
        emailVerificationService.sendVerificationEmail(savedUser);

        // Return response without token (user needs to verify email first)
        return new RegisterResponse(
                savedUser.getEmail(),
                savedUser.getFirstName(),
                "Un email de verification a ete envoye a " + savedUser.getEmail()
        );
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String clientIp) {
        String email = request.email();

        // Check if account is locked due to too many failed attempts
        if (loginAttemptService.isBlocked(email, clientIp)) {
            long remainingSeconds = loginAttemptService.getRemainingLockoutSeconds(email, clientIp);
            log.warn("Login blocked - account locked: email={}, ip={}, remainingSeconds={}",
                    maskEmail(email), clientIp, remainingSeconds);
            throw new AccountLockedException(
                    "Compte temporairement verrouille suite a trop de tentatives. Reessayez dans " +
                    (remainingSeconds / 60 + 1) + " minutes.",
                    remainingSeconds
            );
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password())
            );
        } catch (BadCredentialsException e) {
            // Record failed attempt
            loginAttemptService.recordFailedAttempt(email, clientIp);
            log.warn("Failed login attempt: email={}, ip={}", maskEmail(email), clientIp);
            throw new IllegalArgumentException("Email ou mot de passe incorrect");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Email ou mot de passe incorrect"));

        // Check if email is verified
        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new EmailNotVerifiedException(
                    "Veuillez verifier votre email avant de vous connecter.",
                    user.getEmail()
            );
        }

        // Check if account is suspended
        if (Boolean.TRUE.equals(user.getIsSuspended())) {
            log.warn("Login attempt on suspended account: email={}, ip={}", maskEmail(email), clientIp);
            throw new AccountSuspendedException("Votre compte a ete suspendu. Contactez l'administrateur pour plus d'informations.");
        }

        // Successful login - clear failed attempts
        loginAttemptService.recordSuccessfulLogin(email, clientIp);

        // Update last login timestamp
        user.setLastLoginAt(java.time.LocalDateTime.now());
        userRepository.save(user);

        UserDetailsImpl userDetails = new UserDetailsImpl(user);
        String token = jwtService.generateToken(userDetails);

        log.info("Successful login: userId={}, role={}, ip={}", user.getId(), user.getRole(), clientIp);

        return new AuthResponse(
                token,
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole()
        );
    }

    // Backward compatibility method
    @Transactional
    public AuthResponse login(LoginRequest request) {
        return login(request, "unknown");
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        int atIndex = email.indexOf('@');
        if (atIndex <= 2) return "***" + email.substring(atIndex);
        return email.substring(0, 2) + "***" + email.substring(atIndex);
    }

    public AuthResponse adminLogin(AdminLoginRequest request) {
        // Authenticate via standard auth manager (uses DB credentials)
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        // Find the user by email (username is email for admin)
        User admin = userRepository.findByEmail(request.username())
                .orElseThrow(() -> new IllegalArgumentException("Identifiants incorrects"));

        // Verify it's an admin account
        if (admin.getRole() != UserRole.ADMIN) {
            throw new IllegalArgumentException("Acces non autorise - compte admin requis");
        }

        // Check if account is suspended
        if (Boolean.TRUE.equals(admin.getIsSuspended())) {
            throw new AccountSuspendedException("Ce compte admin a ete suspendu.");
        }

        // Update last login timestamp
        admin.setLastLoginAt(java.time.LocalDateTime.now());
        userRepository.save(admin);

        UserDetailsImpl userDetails = new UserDetailsImpl(admin);
        String token = jwtService.generateToken(userDetails);

        return new AuthResponse(
                token,
                admin.getId(),
                admin.getEmail(),
                admin.getFirstName(),
                admin.getLastName(),
                admin.getRole()
        );
    }
}
