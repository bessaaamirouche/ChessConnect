package com.chessconnect.controller;

import com.chessconnect.model.Lesson;
import com.chessconnect.model.User;
import com.chessconnect.repository.LessonRepository;
import com.chessconnect.repository.UserRepository;
import com.chessconnect.service.BunnyStorageService;
import com.chessconnect.service.BunnyStreamService;
import com.chessconnect.service.ThumbnailService;
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
    private final ThumbnailService thumbnailService;

    @Value("${jibri.webhook-secret:}")
    private String webhookSecret;

    public RecordingController(LessonRepository lessonRepository, UserRepository userRepository,
                               BunnyStreamService bunnyStreamService, BunnyStorageService bunnyStorageService,
                               ThumbnailService thumbnailService) {
        this.lessonRepository = lessonRepository;
        this.userRepository = userRepository;
        this.bunnyStreamService = bunnyStreamService;
        this.bunnyStorageService = bunnyStorageService;
        this.thumbnailService = thumbnailService;
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
        String filePath = payload.get("path");

        log.info("Recording webhook received: room={}, filename={}, path={}", roomName, filename, filePath);

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

        // Use the exact file path from the webhook (Jibri uses UUID directories, not room names)
        File localFile = null;
        if (filePath != null && !filePath.isBlank()) {
            try {
                File candidateFile = new File(filePath);
                // Security: ensure the path is under the recordings base directory
                String canonicalPath = candidateFile.getCanonicalPath();
                String basePath = new File(RECORDINGS_BASE_PATH).getCanonicalPath();
                if (candidateFile.exists() && candidateFile.isFile() && canonicalPath.startsWith(basePath)) {
                    localFile = candidateFile;
                    log.info("Using file from webhook path: {} ({}KB)", filePath, localFile.length() / 1024);
                } else {
                    log.warn("Webhook path not valid or not under recordings dir: {}", filePath);
                }
            } catch (Exception e) {
                log.warn("Error validating webhook file path: {}", e.getMessage());
            }
        }
        // Fallback: search by lesson ID pattern (legacy)
        if (localFile == null) {
            localFile = findRecordingFile(lessonId);
        }

        // Pre-compute video title while still in JPA session (avoids LazyInitializationException in async thread)
        String videoTitle = "Cours " + lessonId;
        try {
            videoTitle = "Cours " + lessonId + " - " + lesson.getTeacher().getFirstName()
                    + " & " + (lesson.getStudent() != null ? lesson.getStudent().getFirstName() : "Groupe");
        } catch (Exception e) {
            // Lazy loading might fail, use simple title
        }
        final String finalVideoTitle = videoTitle;

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
                        // Re-fetch lesson to avoid race conditions with concurrent webhooks
                        Lesson freshLesson = lessonRepository.findById(finalLessonId).orElse(null);
                        if (freshLesson == null) return;

                        freshLesson.addRecordingSegment(cdnUrl);
                        if (freshLesson.getRecordingUrl() == null) {
                            freshLesson.setRecordingUrl(cdnUrl);
                        }

                        lessonRepository.save(freshLesson);
                        log.info("Recording segment added for lesson {} (total segments: {}): {}",
                                finalLessonId, freshLesson.getRecordingSegmentsList().size(), cdnUrl);

                        // Generate thumbnail from local file (keep file for concatenation)
                        thumbnailService.generateThumbnailFromLocalFile(finalLessonId, finalLocalFile);
                    } else {
                        log.warn("Failed to upload to Bunny CDN, trying Bunny Stream for lesson {}", finalLessonId);
                        uploadToBunnyStreamFallback(finalVideoTitle, finalLocalFile, finalLessonId, videoUrl, roomName, filename);
                    }
                } catch (Exception e) {
                    log.error("Error uploading to Bunny CDN for lesson {}", finalLessonId, e);
                    uploadToBunnyStreamFallback(finalVideoTitle, finalLocalFile, finalLessonId, videoUrl, roomName, filename);
                }
            });

            log.info("Recording upload to Bunny CDN initiated for lesson {}", lessonId);
            return ResponseEntity.ok().body("Recording upload initiated for lesson " + lessonId);
        }

        // Priority 2: Upload to Bunny Stream if Bunny Storage not configured
        if (bunnyStreamService.isConfigured() && localFile != null && localFile.exists()) {
            final Long finalLessonId = lessonId;
            final File finalLocalFile = localFile;
            CompletableFuture.runAsync(() -> {
                uploadToBunnyStreamFallback(finalVideoTitle, finalLocalFile, finalLessonId, videoUrl, roomName, filename);
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

        // Re-fetch lesson to avoid race conditions
        Lesson freshLesson = lessonRepository.findById(lessonId).orElse(lesson);

        freshLesson.addRecordingSegment(recordingUrl);
        if (freshLesson.getRecordingUrl() == null) {
            freshLesson.setRecordingUrl(recordingUrl);
        }

        lessonRepository.save(freshLesson);

        log.info("Recording segment added for lesson {} (total segments: {}): {}",
                lessonId, freshLesson.getRecordingSegmentsList().size(), recordingUrl);
        return ResponseEntity.ok().body("Recording segment added for lesson " + lessonId);
    }

    /**
     * Fallback method to upload to Bunny Stream if Bunny Storage fails.
     * @param videoTitle pre-computed title (avoids LazyInitializationException in async threads)
     */
    private void uploadToBunnyStreamFallback(String videoTitle, File localFile, Long lessonId,
                                              String videoUrl, String roomName, String filename) {
        try {
            if (bunnyStreamService.isConfigured()) {
                String bunnyUrl = bunnyStreamService.uploadAndGetUrl(videoTitle, localFile);
                if (bunnyUrl != null) {
                    // Re-fetch lesson after potentially long upload
                    Lesson lesson = lessonRepository.findById(lessonId).orElse(null);
                    if (lesson == null) return;

                    lesson.addRecordingSegment(bunnyUrl);
                    if (lesson.getRecordingUrl() == null) {
                        lesson.setRecordingUrl(bunnyUrl);
                    }

                    lessonRepository.save(lesson);
                    log.info("Recording segment added for lesson {} (total segments: {}): {}",
                            lessonId, lesson.getRecordingSegmentsList().size(), bunnyUrl);
                    return;
                }
            }

            // Final fallback to local URL
            log.warn("Failed to upload to Bunny Stream, keeping local URL for lesson {}", lessonId);
            Lesson lesson = lessonRepository.findById(lessonId).orElse(null);
            if (lesson == null) return;

            String localUrl = sanitizeAndValidateUrl(videoUrl, roomName, filename);
            if (localUrl != null) {
                lesson.addRecordingSegment(localUrl);
                if (lesson.getRecordingUrl() == null) {
                    lesson.setRecordingUrl(localUrl);
                }
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

        // Filename patterns to match (Jibri uses UUID directories, so match by filename)
        String chessconnectPrefix = "chessconnect-" + lessonId + "-";
        String mychessPrefix = "mychess-lesson-" + lessonId;

        // Scan ALL subdirectories for mp4 files matching the lesson ID
        File[] allDirs = baseDir.listFiles(File::isDirectory);
        if (allDirs == null) {
            return null;
        }

        File latestFile = null;
        for (File directory : allDirs) {
            File[] mp4Files = directory.listFiles((dir, name) ->
                name.endsWith(".mp4") && (
                    name.startsWith(chessconnectPrefix) ||
                    name.startsWith(mychessPrefix)
                )
            );
            if (mp4Files != null) {
                for (File mp4File : mp4Files) {
                    if (latestFile == null || mp4File.lastModified() > latestFile.lastModified()) {
                        latestFile = mp4File;
                    }
                }
            }
        }

        return latestFile;
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
