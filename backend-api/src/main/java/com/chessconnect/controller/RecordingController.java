package com.chessconnect.controller;

import com.chessconnect.model.Lesson;
import com.chessconnect.model.User;
import com.chessconnect.repository.LessonRepository;
import com.chessconnect.repository.UserRepository;
import com.chessconnect.service.BunnyStorageService;
import com.chessconnect.service.BunnyStreamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/recordings")
public class RecordingController {

    private static final Logger log = LoggerFactory.getLogger(RecordingController.class);
    private static final String RECORDINGS_BASE_PATH = "/var/jibri/recordings";

    // Whitelist of allowed domains for recording URLs
    private static final Set<String> ALLOWED_URL_DOMAINS = Set.of(
            "meet.mychess.fr",
            "mychess.fr",
            "vz-34fe20be-093.b-cdn.net",  // Bunny Stream CDN
            "mychess.b-cdn.net"           // Bunny Storage CDN
    );

    // Pattern for valid filenames (alphanumeric, hyphens, underscores, .mp4 extension)
    private static final Pattern VALID_FILENAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+\\.mp4$");

    // Pattern for valid room names
    private static final Pattern VALID_ROOM_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");

    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;
    private final BunnyStreamService bunnyStreamService;
    private final BunnyStorageService bunnyStorageService;

    @Value("${jibri.webhook-secret:}")
    private String webhookSecret;

    public RecordingController(LessonRepository lessonRepository, UserRepository userRepository,
                               BunnyStreamService bunnyStreamService, BunnyStorageService bunnyStorageService) {
        this.lessonRepository = lessonRepository;
        this.userRepository = userRepository;
        this.bunnyStreamService = bunnyStreamService;
        this.bunnyStorageService = bunnyStorageService;
    }

    /**
     * Webhook called by Jibri when a recording is finished.
     * The finalize.sh script sends: {"filename": "xxx.mp4", "path": "/var/jibri/recordings/roomname/xxx.mp4", "room": "roomname"}
     *
     * Security: Requires HMAC signature validation via X-Jibri-Signature header
     */
    @PostMapping("/webhook")
    public ResponseEntity<?> handleRecordingWebhook(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Jibri-Signature", required = false) String signature) {

        // Verify webhook signature if secret is configured
        if (webhookSecret != null && !webhookSecret.isBlank()) {
            if (signature == null || !verifySignature(rawBody, signature)) {
                log.warn("Invalid or missing webhook signature");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
            }
        } else {
            log.warn("Webhook secret not configured - webhook authentication disabled");
        }

        // Parse the JSON payload
        Map<String, String> payload;
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            payload = mapper.readValue(rawBody, new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            log.warn("Invalid JSON payload: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Invalid JSON payload");
        }

        String filename = payload.get("filename");
        String roomName = payload.get("room");
        String videoUrl = payload.get("url");

        log.info("Recording webhook received: room={}, filename={}, url={}", roomName, filename, videoUrl);

        // Validate required fields
        if (roomName == null || filename == null) {
            log.warn("Invalid webhook payload: missing room or filename");
            return ResponseEntity.badRequest().body("Missing room or filename");
        }

        // Sanitize and validate inputs
        if (!isValidRoomName(roomName)) {
            log.warn("Invalid room name format: {}", roomName);
            return ResponseEntity.badRequest().body("Invalid room name format");
        }

        if (!isValidFilename(filename)) {
            log.warn("Invalid filename format: {}", filename);
            return ResponseEntity.badRequest().body("Invalid filename format");
        }

        // Extract lesson ID from room name (format: "mychess-lesson-{id}" or "chessconnect-{id}-{timestamp}")
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

        // Find the local video file
        File localFile = findRecordingFile(lessonId);

        // Priority 1: Upload to Bunny Storage CDN (simple file hosting)
        if (bunnyStorageService.isConfigured() && localFile != null && localFile.exists()) {
            final Long finalLessonId = lessonId;
            final File finalLocalFile = localFile;

            // Upload asynchronously to not block the webhook response
            CompletableFuture.runAsync(() -> {
                try {
                    String cdnFilename = bunnyStorageService.generateRecordingFilename(finalLessonId);
                    byte[] fileBytes = Files.readAllBytes(finalLocalFile.toPath());
                    String cdnUrl = bunnyStorageService.uploadRecording(fileBytes, cdnFilename);

                    if (cdnUrl != null) {
                        lesson.setRecordingUrl(cdnUrl);
                        lessonRepository.save(lesson);
                        log.info("Recording uploaded to Bunny CDN for lesson {}: {}", finalLessonId, cdnUrl);

                        // Delete local file after successful upload
                        try {
                            Files.deleteIfExists(finalLocalFile.toPath());
                            log.debug("Deleted local recording file after CDN upload: {}", finalLocalFile.getPath());
                        } catch (Exception e) {
                            log.warn("Could not delete local file after CDN upload: {}", e.getMessage());
                        }
                    } else {
                        log.warn("Failed to upload to Bunny CDN, trying Bunny Stream for lesson {}", finalLessonId);
                        // Fall back to Bunny Stream
                        uploadToBunnyStreamFallback(lesson, finalLocalFile, finalLessonId, videoUrl, roomName, filename);
                    }
                } catch (Exception e) {
                    log.error("Error uploading to Bunny CDN for lesson {}", finalLessonId, e);
                    // Fall back to Bunny Stream
                    uploadToBunnyStreamFallback(lesson, finalLocalFile, finalLessonId, videoUrl, roomName, filename);
                }
            });

            log.info("Recording upload to Bunny CDN initiated for lesson {}", lessonId);
            return ResponseEntity.ok().body("Recording upload initiated for lesson " + lessonId);
        }

        // Priority 2: Upload to Bunny Stream if Bunny Storage not configured
        if (bunnyStreamService.isConfigured() && localFile != null && localFile.exists()) {
            final Long finalLessonId = lessonId;
            CompletableFuture.runAsync(() -> {
                uploadToBunnyStreamFallback(lesson, localFile, finalLessonId, videoUrl, roomName, filename);
            });

            log.info("Recording upload to Bunny Stream initiated for lesson {}", lessonId);
            return ResponseEntity.ok().body("Recording upload initiated for lesson " + lessonId);
        }

        // Fall back to local URL if no Bunny service configured or file not found
        String recordingUrl = sanitizeAndValidateUrl(videoUrl, roomName, filename);
        if (recordingUrl == null) {
            log.warn("Invalid video URL provided: {}", videoUrl);
            return ResponseEntity.badRequest().body("Invalid video URL");
        }

        lesson.setRecordingUrl(recordingUrl);
        lessonRepository.save(lesson);

        log.info("Recording linked to lesson {}: {}", lessonId, recordingUrl);
        return ResponseEntity.ok().body("Recording linked to lesson " + lessonId);
    }

    /**
     * Fallback method to upload to Bunny Stream if Bunny Storage fails.
     */
    private void uploadToBunnyStreamFallback(Lesson lesson, File localFile, Long lessonId,
                                              String videoUrl, String roomName, String filename) {
        try {
            if (bunnyStreamService.isConfigured()) {
                String title = "Cours " + lessonId + " - " + lesson.getTeacher().getFirstName() + " & " + lesson.getStudent().getFirstName();
                String bunnyUrl = bunnyStreamService.uploadAndGetUrl(title, localFile);
                if (bunnyUrl != null) {
                    lesson.setRecordingUrl(bunnyUrl);
                    lessonRepository.save(lesson);
                    log.info("Recording uploaded to Bunny Stream for lesson {}: {}", lessonId, bunnyUrl);
                    return;
                }
            }

            // Final fallback to local URL
            log.warn("Failed to upload to Bunny Stream, keeping local URL for lesson {}", lessonId);
            String localUrl = sanitizeAndValidateUrl(videoUrl, roomName, filename);
            if (localUrl != null) {
                lesson.setRecordingUrl(localUrl);
                lessonRepository.save(lesson);
            }
        } catch (Exception e) {
            log.error("Error in Bunny Stream fallback for lesson {}", lessonId, e);
        }
    }

    /**
     * Serve a recording video file for a lesson.
     * Security: Only the student or teacher of the lesson can access the video.
     */
    @GetMapping("/video/{lessonId}")
    public ResponseEntity<Resource> getRecordingVideo(@PathVariable Long lessonId) {
        // Get authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String userEmail = authentication.getName();
        User user = userRepository.findByEmail(userEmail).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Lesson lesson = lessonRepository.findById(lessonId).orElse(null);
        if (lesson == null) {
            return ResponseEntity.notFound().build();
        }

        // ACL check: only student or teacher of this lesson can access
        boolean isStudent = lesson.getStudent().getId().equals(user.getId());
        boolean isTeacher = lesson.getTeacher().getId().equals(user.getId());
        boolean isAdmin = "ADMIN".equals(user.getRole().name());

        if (!isStudent && !isTeacher && !isAdmin) {
            log.warn("User {} attempted to access recording for lesson {} without authorization",
                    user.getId(), lessonId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
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

    /**
     * Verify HMAC-SHA256 signature of webhook payload.
     */
    private boolean verifySignature(String payload, String signature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = "sha256=" + HexFormat.of().formatHex(hash);
            return expectedSignature.equalsIgnoreCase(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error verifying webhook signature", e);
            return false;
        }
    }

    /**
     * Validate room name format (prevent path traversal).
     */
    private boolean isValidRoomName(String roomName) {
        if (roomName == null || roomName.length() > 100) {
            return false;
        }
        // Must match alphanumeric, hyphens, underscores only
        return VALID_ROOM_PATTERN.matcher(roomName).matches();
    }

    /**
     * Validate filename format (prevent path traversal).
     */
    private boolean isValidFilename(String filename) {
        if (filename == null || filename.length() > 100) {
            return false;
        }
        // Must be a valid .mp4 filename
        return VALID_FILENAME_PATTERN.matcher(filename).matches();
    }

    /**
     * Sanitize and validate the video URL.
     * Only allows URLs from whitelisted domains.
     */
    private String sanitizeAndValidateUrl(String videoUrl, String roomName, String filename) {
        // If URL provided, validate it
        if (videoUrl != null && !videoUrl.isBlank()) {
            try {
                java.net.URI uri = new java.net.URI(videoUrl);
                String host = uri.getHost();
                if (host != null && ALLOWED_URL_DOMAINS.contains(host.toLowerCase())) {
                    // URL is from allowed domain
                    return videoUrl;
                }
                log.warn("Video URL from non-whitelisted domain: {}", host);
            } catch (Exception e) {
                log.warn("Invalid video URL format: {}", videoUrl);
            }
        }

        // Construct a safe URL from validated components
        return "https://meet.mychess.fr/recordings/" + roomName + "/" + filename;
    }

    private File findRecordingFile(Long lessonId) {
        File baseDir = new File(RECORDINGS_BASE_PATH);
        if (!baseDir.exists() || !baseDir.isDirectory()) {
            return null;
        }

        // Search for directories matching patterns:
        // - "Lesson-{id}"
        // - "ChessConnect_Lesson-{id}"
        // - "chessconnect-{id}-*" (old format with timestamp)
        // - "mychess-lesson-{id}"
        File[] matchingDirs = baseDir.listFiles((dir, name) -> {
            if (name.equals("Lesson-" + lessonId)) return true;
            if (name.equals("ChessConnect_Lesson-" + lessonId)) return true;
            if (name.startsWith("chessconnect-" + lessonId + "-")) return true;
            if (name.equals("mychess-lesson-" + lessonId)) return true;
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
