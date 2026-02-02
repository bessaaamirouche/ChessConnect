package com.chessconnect.controller;

import com.chessconnect.model.Lesson;
import com.chessconnect.model.User;
import com.chessconnect.model.VideoWatchProgress;
import com.chessconnect.repository.LessonRepository;
import com.chessconnect.repository.UserRepository;
import com.chessconnect.repository.VideoWatchProgressRepository;
import com.chessconnect.security.UserDetailsImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/video-progress")
public class VideoProgressController {

    private final VideoWatchProgressRepository progressRepository;
    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;

    public VideoProgressController(
            VideoWatchProgressRepository progressRepository,
            LessonRepository lessonRepository,
            UserRepository userRepository) {
        this.progressRepository = progressRepository;
        this.lessonRepository = lessonRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/{lessonId}")
    public ResponseEntity<?> getProgress(
            @PathVariable Long lessonId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifie"));
        }

        Optional<VideoWatchProgress> progress = progressRepository.findByUserIdAndLessonId(userDetails.getId(), lessonId);

        if (progress.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                "lessonId", lessonId,
                "watchPosition", 0,
                "duration", 0,
                "completed", false
            ));
        }

        VideoWatchProgress p = progress.get();
        return ResponseEntity.ok(Map.of(
            "lessonId", lessonId,
            "watchPosition", p.getWatchPosition(),
            "duration", p.getDuration() != null ? p.getDuration() : 0,
            "completed", p.getCompleted(),
            "updatedAt", p.getUpdatedAt().toString()
        ));
    }

    @PostMapping("/{lessonId}")
    public ResponseEntity<?> saveProgress(
            @PathVariable Long lessonId,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifie"));
        }

        // Verify the lesson exists and user has access
        Optional<Lesson> lessonOpt = lessonRepository.findById(lessonId);
        if (lessonOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Cours non trouve"));
        }

        Lesson lesson = lessonOpt.get();
        // Check if user is the student or teacher of this lesson
        if (!lesson.getStudent().getId().equals(userDetails.getId()) &&
            !lesson.getTeacher().getId().equals(userDetails.getId())) {
            return ResponseEntity.status(403).body(Map.of("error", "Acces non autorise"));
        }

        Double watchPosition = body.get("watchPosition") != null
            ? ((Number) body.get("watchPosition")).doubleValue()
            : 0.0;
        Double duration = body.get("duration") != null
            ? ((Number) body.get("duration")).doubleValue()
            : null;
        Boolean completed = body.get("completed") != null
            ? (Boolean) body.get("completed")
            : false;

        // Find existing or create new progress
        VideoWatchProgress progress = progressRepository
            .findByUserIdAndLessonId(userDetails.getId(), lessonId)
            .orElseGet(() -> {
                User user = userRepository.findById(userDetails.getId()).orElseThrow();
                return new VideoWatchProgress(user, lesson);
            });

        progress.setWatchPosition(watchPosition);
        if (duration != null) {
            progress.setDuration(duration);
        }
        progress.setCompleted(completed);

        progressRepository.save(progress);

        return ResponseEntity.ok(Map.of(
            "success", true,
            "lessonId", lessonId,
            "watchPosition", progress.getWatchPosition()
        ));
    }

    @DeleteMapping("/{lessonId}")
    public ResponseEntity<?> deleteProgress(
            @PathVariable Long lessonId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifie"));
        }

        progressRepository.deleteByUserIdAndLessonId(userDetails.getId(), lessonId);

        return ResponseEntity.ok(Map.of("success", true));
    }
}
