package com.chessconnect.controller;

import com.chessconnect.model.Lesson;
import com.chessconnect.repository.LessonRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/recordings")
public class RecordingController {

    private static final Logger log = LoggerFactory.getLogger(RecordingController.class);
    private static final String RECORDINGS_BASE_PATH = "/var/jibri/recordings";

    private final LessonRepository lessonRepository;

    public RecordingController(LessonRepository lessonRepository) {
        this.lessonRepository = lessonRepository;
    }

    /**
     * Webhook called by Jibri when a recording is finished.
     * The finalize.sh script sends: {"filename": "xxx.mp4", "path": "/var/jibri/recordings/roomname/xxx.mp4", "room": "roomname"}
     */
    @PostMapping("/webhook")
    public ResponseEntity<?> handleRecordingWebhook(@RequestBody Map<String, String> payload) {
        String filename = payload.get("filename");
        String filePath = payload.get("path");
        String roomName = payload.get("room");

        log.info("Recording webhook received: room={}, filename={}, path={}", roomName, filename, filePath);

        if (roomName == null || filename == null) {
            log.warn("Invalid webhook payload: missing room or filename");
            return ResponseEntity.badRequest().body("Missing room or filename");
        }

        // Extract lesson ID from room name (format: "Lesson-{id}" or "ChessConnect_Lesson-{id}")
        Long lessonId = extractLessonId(roomName);
        if (lessonId == null) {
            log.warn("Could not extract lesson ID from room name: {}", roomName);
            return ResponseEntity.ok().body("Room name not linked to a lesson");
        }

        // Find the lesson and update recording URL
        Lesson lesson = lessonRepository.findById(lessonId).orElse(null);
        if (lesson == null) {
            log.warn("Lesson not found: {}", lessonId);
            return ResponseEntity.ok().body("Lesson not found");
        }

        // Set the recording URL (relative path that will be served by the /recordings/video endpoint)
        String recordingUrl = "/api/recordings/video/" + lessonId;
        lesson.setRecordingUrl(recordingUrl);
        lessonRepository.save(lesson);

        log.info("Recording linked to lesson {}: {}", lessonId, recordingUrl);
        return ResponseEntity.ok().body("Recording linked to lesson " + lessonId);
    }

    /**
     * Serve a recording video file for a lesson.
     */
    @GetMapping("/video/{lessonId}")
    public ResponseEntity<Resource> getRecordingVideo(@PathVariable Long lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId).orElse(null);
        if (lesson == null) {
            return ResponseEntity.notFound().build();
        }

        // Find the recording file
        // Room name format: "Lesson-{id}" or "ChessConnect_Lesson-{id}"
        String[] possibleRoomNames = {
            "Lesson-" + lessonId,
            "ChessConnect_Lesson-" + lessonId
        };

        File recordingFile = null;
        for (String roomName : possibleRoomNames) {
            Path roomDir = Paths.get(RECORDINGS_BASE_PATH, roomName);
            if (Files.exists(roomDir)) {
                File[] mp4Files = roomDir.toFile().listFiles((dir, name) -> name.endsWith(".mp4"));
                if (mp4Files != null && mp4Files.length > 0) {
                    recordingFile = mp4Files[0]; // Take the first mp4 file
                    break;
                }
            }
        }

        if (recordingFile == null || !recordingFile.exists()) {
            log.warn("Recording file not found for lesson {}", lessonId);
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(recordingFile);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("video/mp4"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"lesson-" + lessonId + ".mp4\"")
                .body(resource);
    }

    private Long extractLessonId(String roomName) {
        // Try patterns: "Lesson-123", "ChessConnect_Lesson-123"
        Pattern pattern = Pattern.compile("Lesson-(\\d+)");
        Matcher matcher = pattern.matcher(roomName);
        if (matcher.find()) {
            try {
                return Long.parseLong(matcher.group(1));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
