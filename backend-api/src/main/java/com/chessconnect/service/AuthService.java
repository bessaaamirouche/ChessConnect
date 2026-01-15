package com.chessconnect.service;

import com.chessconnect.dto.auth.AuthResponse;
import com.chessconnect.dto.auth.LoginRequest;
import com.chessconnect.dto.auth.RegisterRequest;
import com.chessconnect.model.Progress;
import com.chessconnect.model.User;
import com.chessconnect.model.enums.ChessLevel;
import com.chessconnect.model.enums.UserRole;
import com.chessconnect.repository.ProgressRepository;
import com.chessconnect.repository.UserRepository;
import com.chessconnect.security.JwtService;
import com.chessconnect.security.UserDetailsImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final ProgressRepository progressRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(
            UserRepository userRepository,
            ProgressRepository progressRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager
    ) {
        this.userRepository = userRepository;
        this.progressRepository = progressRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered");
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setRole(request.role());

        if (request.role() == UserRole.TEACHER) {
            user.setHourlyRateCents(request.hourlyRateCents() != null ? request.hourlyRateCents() : 5000);
            user.setAcceptsSubscription(request.acceptsSubscription() != null ? request.acceptsSubscription() : true);
            user.setBio(request.bio());
        } else if (request.role() == UserRole.STUDENT) {
            user.setBirthDate(request.birthDate());
            user.setEloRating(request.eloRating());
        }

        User savedUser = userRepository.save(user);

        if (request.role() == UserRole.STUDENT) {
            Progress progress = new Progress();
            progress.setStudent(savedUser);
            progress.setCurrentLevel(ChessLevel.PION);
            progressRepository.save(progress);
        }

        UserDetailsImpl userDetails = new UserDetailsImpl(savedUser);
        String token = jwtService.generateToken(userDetails);

        return new AuthResponse(
                token,
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getFirstName(),
                savedUser.getLastName(),
                savedUser.getRole()
        );
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        UserDetailsImpl userDetails = new UserDetailsImpl(user);
        String token = jwtService.generateToken(userDetails);

        return new AuthResponse(
                token,
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole()
        );
    }
}
