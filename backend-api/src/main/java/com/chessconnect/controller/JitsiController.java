package com.chessconnect.controller;

import com.chessconnect.dto.jitsi.JitsiTokenResponse;
import com.chessconnect.model.Lesson;
import com.chessconnect.model.User;
import com.chessconnect.model.enums.UserRole;
import com.chessconnect.repository.LessonRepository;
import com.chessconnect.repository.UserRepository;
import com.chessconnect.service.JitsiTokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/jitsi")
public class JitsiController {

    private static final Pattern ROOM_PATTERN = Pattern.compile("^mychess-lesson-(\\d+)$");

    private final JitsiTokenService jitsiTokenService;
    private final UserRepository userRepository;
    private final LessonRepository lessonRepository;

    public JitsiController(JitsiTokenService jitsiTokenService, UserRepository userRepository, LessonRepository lessonRepository) {
        this.jitsiTokenService = jitsiTokenService;
        this.userRepository = userRepository;
        this.lessonRepository = lessonRepository;
    }

    @GetMapping("/token")
    public ResponseEntity<JitsiTokenResponse> getToken(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String roomName
    ) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouve"));

        // Validate room name format and check user is participant of this lesson
        Matcher matcher = ROOM_PATTERN.matcher(roomName);
        if (matcher.matches()) {
            Long lessonId = Long.parseLong(matcher.group(1));
            Lesson lesson = lessonRepository.findById(lessonId).orElse(null);
            if (lesson != null && user.getRole() != UserRole.ADMIN) {
                boolean isParticipant = lesson.getStudent().getId().equals(user.getId())
                        || lesson.getTeacher().getId().equals(user.getId());
                if (!isParticipant) {
                    return ResponseEntity.status(403).build();
                }
            }
        } else if (!roomName.matches("^[a-zA-Z0-9-_]{1,100}$")) {
            return ResponseEntity.badRequest().build();
        }

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
