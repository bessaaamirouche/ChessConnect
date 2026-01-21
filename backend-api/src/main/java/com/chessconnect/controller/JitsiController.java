package com.chessconnect.controller;

import com.chessconnect.dto.jitsi.JitsiTokenResponse;
import com.chessconnect.model.User;
import com.chessconnect.model.enums.UserRole;
import com.chessconnect.repository.UserRepository;
import com.chessconnect.service.JitsiTokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/jitsi")
public class JitsiController {

    private final JitsiTokenService jitsiTokenService;
    private final UserRepository userRepository;

    public JitsiController(JitsiTokenService jitsiTokenService, UserRepository userRepository) {
        this.jitsiTokenService = jitsiTokenService;
        this.userRepository = userRepository;
    }

    @GetMapping("/token")
    public ResponseEntity<JitsiTokenResponse> getToken(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String roomName
    ) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouve"));

        String token = jitsiTokenService.generateToken(user, roomName);
        boolean isModerator = user.getRole() == UserRole.TEACHER || user.getRole() == UserRole.ADMIN;

        return ResponseEntity.ok(new JitsiTokenResponse(
                token,
                roomName,
                "meet.mychess.fr",
                isModerator
        ));
    }
}
