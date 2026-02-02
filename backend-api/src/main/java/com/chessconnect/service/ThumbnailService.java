package com.chessconnect.service;

import com.chessconnect.model.Lesson;
import com.chessconnect.repository.LessonRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@Service
public class ThumbnailService {

    private static final Logger log = LoggerFactory.getLogger(ThumbnailService.class);

    private static final String THUMBNAIL_DIR = "/app/uploads/thumbnails";
    private static final String THUMBNAIL_TIME = "00:00:30"; // Extract frame at 30 seconds
    private static final int DOWNLOAD_TIMEOUT = 120000; // 2 minutes for download

    @Value("${app.base-url:http://localhost:8282}")
    private String baseUrl;

    private final LessonRepository lessonRepository;
    private final BunnyStorageService bunnyStorageService;

    public ThumbnailService(LessonRepository lessonRepository, BunnyStorageService bunnyStorageService) {
        this.lessonRepository = lessonRepository;
        this.bunnyStorageService = bunnyStorageService;
        // Ensure thumbnail directory exists
        try {
            Files.createDirectories(Paths.get(THUMBNAIL_DIR));
        } catch (IOException e) {
            log.error("Failed to create thumbnail directory", e);
        }
    }

    /**
     * Generate a thumbnail for a lesson's recording asynchronously.
     * Supports both local files and remote URLs (Bunny CDN).
     */
    @Async
    public void generateThumbnailAsync(Long lessonId) {
        try {
            Lesson lesson = lessonRepository.findById(lessonId).orElse(null);
            if (lesson == null || lesson.getRecordingUrl() == null) {
                log.warn("Cannot generate thumbnail: lesson {} not found or has no recording", lessonId);
                return;
            }

            // Skip if thumbnail already exists
            if (lesson.getThumbnailUrl() != null) {
                log.info("Thumbnail already exists for lesson {}", lessonId);
                return;
            }

            String recordingUrl = lesson.getRecordingUrl();
            String thumbnailFilename = "thumbnail_" + lessonId + ".jpg";
            Path thumbnailPath = Paths.get(THUMBNAIL_DIR, thumbnailFilename);

            log.info("Generating thumbnail for lesson {} from {}", lessonId, recordingUrl);

            boolean success;

            // Check if it's a remote URL or local file
            if (recordingUrl.startsWith("http://") || recordingUrl.startsWith("https://")) {
                // Remote URL - FFmpeg can handle it directly for some URLs
                // For Bunny CDN, we may need to download first
                if (recordingUrl.contains(".b-cdn.net") || recordingUrl.contains("bunnycdn")) {
                    // Bunny CDN - try direct FFmpeg access first
                    success = extractThumbnailFromUrl(recordingUrl, thumbnailPath.toString());

                    if (!success) {
                        // If FFmpeg can't access directly, download and process
                        log.info("Direct FFmpeg access failed, downloading video for lesson {}", lessonId);
                        success = downloadAndExtractThumbnail(recordingUrl, thumbnailPath.toString(), lessonId);
                    }
                } else {
                    // Other URLs - try direct access
                    success = extractThumbnailFromUrl(recordingUrl, thumbnailPath.toString());
                }
            } else {
                // Local file path
                success = extractThumbnailFromFile(recordingUrl, thumbnailPath.toString());
            }

            if (success && Files.exists(thumbnailPath)) {
                // Upload thumbnail to Bunny Storage if configured
                String thumbnailUrl = uploadThumbnailToCdn(thumbnailPath, thumbnailFilename);

                if (thumbnailUrl == null) {
                    // Fallback to local serving
                    thumbnailUrl = "/api/uploads/thumbnails/" + thumbnailFilename;
                }

                lesson.setThumbnailUrl(thumbnailUrl);
                lessonRepository.save(lesson);
                log.info("Thumbnail generated successfully for lesson {}: {}", lessonId, thumbnailUrl);
            } else {
                log.warn("Failed to generate thumbnail for lesson {}", lessonId);
            }

        } catch (Exception e) {
            log.error("Error generating thumbnail for lesson " + lessonId, e);
        }
    }

    /**
     * Generate thumbnail from a local file.
     */
    public boolean generateThumbnailFromLocalFile(Long lessonId, File videoFile) {
        try {
            Lesson lesson = lessonRepository.findById(lessonId).orElse(null);
            if (lesson == null) {
                return false;
            }

            String thumbnailFilename = "thumbnail_" + lessonId + ".jpg";
            Path thumbnailPath = Paths.get(THUMBNAIL_DIR, thumbnailFilename);

            boolean success = extractThumbnailFromFile(videoFile.getAbsolutePath(), thumbnailPath.toString());

            if (success && Files.exists(thumbnailPath)) {
                String thumbnailUrl = uploadThumbnailToCdn(thumbnailPath, thumbnailFilename);

                if (thumbnailUrl == null) {
                    thumbnailUrl = "/api/uploads/thumbnails/" + thumbnailFilename;
                }

                lesson.setThumbnailUrl(thumbnailUrl);
                lessonRepository.save(lesson);
                log.info("Thumbnail generated from local file for lesson {}: {}", lessonId, thumbnailUrl);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("Error generating thumbnail from local file for lesson " + lessonId, e);
            return false;
        }
    }

    /**
     * Extract thumbnail directly from a URL using FFmpeg.
     */
    private boolean extractThumbnailFromUrl(String videoUrl, String outputPath) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg",
                "-y",
                "-ss", THUMBNAIL_TIME,
                "-i", videoUrl,
                "-frames:v", "1",
                "-vf", "scale=480:-1",
                "-q:v", "2",
                outputPath
            );

            pb.redirectErrorStream(true);
            Process process = pb.start();

            boolean completed = process.waitFor(90, TimeUnit.SECONDS);

            if (!completed) {
                process.destroyForcibly();
                log.error("FFmpeg timed out for URL: {}", videoUrl);
                return false;
            }

            return process.exitValue() == 0;

        } catch (Exception e) {
            log.error("FFmpeg failed for URL: {}", videoUrl, e);
            return false;
        }
    }

    /**
     * Extract thumbnail from a local file using FFmpeg.
     */
    private boolean extractThumbnailFromFile(String videoPath, String outputPath) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg",
                "-y",
                "-i", videoPath,
                "-ss", THUMBNAIL_TIME,
                "-frames:v", "1",
                "-vf", "scale=480:-1",
                "-q:v", "2",
                outputPath
            );

            pb.redirectErrorStream(true);
            Process process = pb.start();

            boolean completed = process.waitFor(60, TimeUnit.SECONDS);

            if (!completed) {
                process.destroyForcibly();
                log.error("FFmpeg timed out for file: {}", videoPath);
                return false;
            }

            return process.exitValue() == 0;

        } catch (Exception e) {
            log.error("FFmpeg failed for file: {}", videoPath, e);
            return false;
        }
    }

    /**
     * Download video and extract thumbnail.
     */
    private boolean downloadAndExtractThumbnail(String videoUrl, String thumbnailPath, Long lessonId) {
        Path tempVideoPath = null;
        try {
            // Create temp file for video
            tempVideoPath = Files.createTempFile("video_" + lessonId + "_", ".mp4");

            // Download video
            log.info("Downloading video from {} for thumbnail generation", videoUrl);
            URL url = new URL(videoUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(DOWNLOAD_TIMEOUT);
            conn.setRequestProperty("User-Agent", "ChessConnect/1.0");

            try (InputStream in = conn.getInputStream();
                 OutputStream out = Files.newOutputStream(tempVideoPath)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }

            log.info("Video downloaded, extracting thumbnail for lesson {}", lessonId);

            // Extract thumbnail from downloaded file
            boolean success = extractThumbnailFromFile(tempVideoPath.toString(), thumbnailPath);

            return success;

        } catch (Exception e) {
            log.error("Error downloading video for thumbnail", e);
            return false;
        } finally {
            // Clean up temp file
            if (tempVideoPath != null) {
                try {
                    Files.deleteIfExists(tempVideoPath);
                } catch (IOException e) {
                    log.warn("Could not delete temp video file: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * Upload thumbnail to Bunny CDN.
     */
    private String uploadThumbnailToCdn(Path thumbnailPath, String filename) {
        if (!bunnyStorageService.isConfigured()) {
            return null;
        }

        try {
            byte[] thumbnailBytes = Files.readAllBytes(thumbnailPath);
            // Use recordings path but for thumbnails subdirectory
            String cdnPath = "thumbnails/" + filename;
            String cdnUrl = bunnyStorageService.uploadRecording(thumbnailBytes, cdnPath);

            if (cdnUrl != null) {
                // Delete local file after successful CDN upload
                Files.deleteIfExists(thumbnailPath);
            }

            return cdnUrl;
        } catch (Exception e) {
            log.error("Failed to upload thumbnail to CDN", e);
            return null;
        }
    }

    /**
     * Check if FFmpeg is available on the system.
     */
    public boolean isFfmpegAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-version");
            Process process = pb.start();
            boolean completed = process.waitFor(5, TimeUnit.SECONDS);
            return completed && process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Generate thumbnails for all lessons that have recordings but no thumbnails.
     */
    public void generateMissingThumbnails() {
        if (!isFfmpegAvailable()) {
            log.warn("FFmpeg not available, cannot generate thumbnails");
            return;
        }

        log.info("Checking for lessons with missing thumbnails...");

        lessonRepository.findAll().stream()
            .filter(l -> l.getRecordingUrl() != null && l.getThumbnailUrl() == null)
            .forEach(lesson -> {
                log.info("Generating missing thumbnail for lesson {}", lesson.getId());
                generateThumbnailAsync(lesson.getId());
            });
    }
}
