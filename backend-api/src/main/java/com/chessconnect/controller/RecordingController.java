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
        String videoUrl = payload.get("url"); // URL directe envoyée par finalize.sh

        log.info("Recording webhook received: room={}, filename={}, path={}, url={}", roomName, filename, filePath, videoUrl);

        if (roomName == null || filename == null) {
            log.warn("Invalid webhook payload: missing room or filename");
            return ResponseEntity.badRequest().body("Missing room or filename");
        }

        // Extract lesson ID from room name (format: "chessconnect-{id}-{timestamp}")
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

        // Use the URL from finalize.sh if provided, otherwise construct it
        String recordingUrl = videoUrl != null ? videoUrl : "https://meet.mychess.fr/recordings/" + roomName + "/" + filename;
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
        File recordingFile = findRecordingFile(lessonId);

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

    private File findRecordingFile(Long lessonId) {
        File baseDir = new File(RECORDINGS_BASE_PATH);
        if (!baseDir.exists() || !baseDir.isDirectory()) {
            return null;
        }

        // Search for directories matching patterns:
        // - "Lesson-{id}"
        // - "ChessConnect_Lesson-{id}"
        // - "chessconnect-{id}-*" (new format with timestamp)
        File[] matchingDirs = baseDir.listFiles((dir, name) -> {
            if (name.equals("Lesson-" + lessonId)) return true;
            if (name.equals("ChessConnect_Lesson-" + lessonId)) return true;
            if (name.startsWith("chessconnect-" + lessonId + "-")) return true;
            return false;
        });

        if (matchingDirs == null || matchingDirs.length == 0) {
            return null;
        }

        // Find the most recent directory (in case of multiple recordings)
        File latestDir = matchingDirs[0];
        for (File dir : matchingDirs) {
            if (dir.lastModified() > latestDir.lastModified()) {
                latestDir = dir;
            }
        }

        // Find mp4 file in the directory
        File[] mp4Files = latestDir.listFiles((dir, name) -> name.endsWith(".mp4"));
        if (mp4Files != null && mp4Files.length > 0) {
            return mp4Files[0];
        }

        return null;
    }

    private Long extractLessonId(String roomName) {
        // Try patterns:
        // - "mychess-lesson-123" (current format)
        // - "Lesson-123"
        // - "ChessConnect_Lesson-123"
        // - "chessconnect-123-1234567890" (old format with timestamp)

        // Try current format first: mychess-lesson-{id}
        Pattern currentPattern = Pattern.compile("mychess-lesson-(\\d+)");
        Matcher currentMatcher = currentPattern.matcher(roomName);
        if (currentMatcher.find()) {
            try {
                return Long.parseLong(currentMatcher.group(1));
            } catch (NumberFormatException e) {
                // Continue to try other patterns
            }
        }

        // Try old format: chessconnect-{id}-{timestamp}
        Pattern newPattern = Pattern.compile("chessconnect-(\\d+)-\\d+");
        Matcher newMatcher = newPattern.matcher(roomName);
        if (newMatcher.find()) {
            try {
                return Long.parseLong(newMatcher.group(1));
            } catch (NumberFormatException e) {
                // Continue to try other patterns
            }
        }

        // Try old format: Lesson-{id}
        Pattern oldPattern = Pattern.compile("Lesson-(\\d+)");
        Matcher oldMatcher = oldPattern.matcher(roomName);
        if (oldMatcher.find()) {
            try {
                return Long.parseLong(oldMatcher.group(1));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
