package com.chessconnect.controller;

import com.chessconnect.dto.jitsi.JitsiTokenResponse;
import com.chessconnect.model.Lesson;
import com.chessconnect.model.User;
import com.chessconnect.model.enums.UserRole;
import com.chessconnect.repository.LessonParticipantRepository;
import com.chessconnect.repository.LessonRepository;
import com.chessconnect.repository.UserRepository;
import com.chessconnect.service.JitsiTokenService;
import com.chessconnect.service.SubscriptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/jitsi")
public class JitsiController {

    // Matches both formats: "mychess-lesson-{id}" and "chessconnect-{id}-{timestamp}"
    private static final Pattern ROOM_PATTERN = Pattern.compile("^(?:mychess-lesson|chessconnect)-(\\d+)(?:-\\d+)?$");

    private final JitsiTokenService jitsiTokenService;
    private final UserRepository userRepository;
    private final LessonRepository lessonRepository;
    private final LessonParticipantRepository participantRepository;
    private final SubscriptionService subscriptionService;

    public JitsiController(JitsiTokenService jitsiTokenService, UserRepository userRepository,
                           LessonRepository lessonRepository, LessonParticipantRepository participantRepository,
                           SubscriptionService subscriptionService) {
        this.jitsiTokenService = jitsiTokenService;
        this.userRepository = userRepository;
        this.lessonRepository = lessonRepository;
        this.participantRepository = participantRepository;
        this.subscriptionService = subscriptionService;
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
                boolean isTeacher = lesson.getTeacher().getId().equals(user.getId());
                boolean isParticipant;
                if (Boolean.TRUE.equals(lesson.getIsGroupLesson())) {
                    // For group lessons, check the participants table
                    isParticipant = isTeacher || participantRepository.existsActiveByLessonIdAndStudentId(lessonId, user.getId());
                } else {
                    isParticipant = isTeacher || lesson.getStudent().getId().equals(user.getId());
                }
                if (!isParticipant) {
                    return ResponseEntity.status(403).build();
                }
            }
        } else if (!roomName.matches("^[a-zA-Z0-9-_]{1,100}$")) {
            return ResponseEntity.badRequest().build();
        }

        boolean isModerator = user.getRole() == UserRole.TEACHER || user.getRole() == UserRole.ADMIN;

        // Check if recording should be enabled (only for premium students)
        boolean recordingEnabled = false;
        Matcher recMatcher = ROOM_PATTERN.matcher(roomName);
        if (recMatcher.matches()) {
            Long recLessonId = Long.parseLong(recMatcher.group(1));
            Lesson recLesson = lessonRepository.findById(recLessonId).orElse(null);
            if (recLesson != null) {
                if (Boolean.TRUE.equals(recLesson.getIsGroupLesson())) {
                    // Group lessons: record only if at least one active participant is premium
                    var activeParticipants = participantRepository.findByLessonIdAndStatus(recLessonId, "ACTIVE");
                    for (var participant : activeParticipants) {
                        if (subscriptionService.isPremium(participant.getStudent().getId())) {
                            recordingEnabled = true;
                            break;
                        }
                    }
                } else if (recLesson.getStudent() != null) {
                    recordingEnabled = subscriptionService.isPremium(recLesson.getStudent().getId());
                }
            }
        }

        String token = jitsiTokenService.generateToken(user, roomName, recordingEnabled);

        return ResponseEntity.ok(new JitsiTokenResponse(
                token,
                roomName,
                "meet.mychess.fr",
                isModerator,
                recordingEnabled
        ));
    }
}
