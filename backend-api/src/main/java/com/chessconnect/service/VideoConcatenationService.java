package com.chessconnect.service;

import com.chessconnect.model.Lesson;
import com.chessconnect.repository.LessonRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for concatenating multiple video segments into a single video file.
 * Used when Jibri creates multiple recordings for the same lesson
 * (e.g., when participants leave/rejoin the room).
 */
@Service
public class VideoConcatenationService {

    private static final Logger log = LoggerFactory.getLogger(VideoConcatenationService.class);
    private static final String RECORDINGS_BASE_PATH = "/var/jibri/recordings";
    private static final String TEMP_CONCAT_DIR = "/tmp/video-concat";

    private final LessonRepository lessonRepository;
    private final BunnyStorageService bunnyStorageService;
    private final BunnyStreamService bunnyStreamService;
    private final ThumbnailService thumbnailService;

    public VideoConcatenationService(LessonRepository lessonRepository,
                                      BunnyStorageService bunnyStorageService,
                                      BunnyStreamService bunnyStreamService,
                                      ThumbnailService thumbnailService) {
        this.lessonRepository = lessonRepository;
        this.bunnyStorageService = bunnyStorageService;
        this.bunnyStreamService = bunnyStreamService;
        this.thumbnailService = thumbnailService;
    }

    /**
     * Concatenate all recording segments for a lesson into a single video.
     * This method:
     * 1. Downloads all segment files from URLs
     * 2. Concatenates them using FFmpeg
     * 3. Uploads the final video to Bunny CDN
     * 4. Updates the lesson with the final recording URL
     * 5. Cleans up temporary files
     *
     * @param lessonId ID of the lesson to process
     * @return true if concatenation was successful, false otherwise
     */
    public boolean concatenateRecordingSegments(Long lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId).orElse(null);
        if (lesson == null) {
            log.warn("Cannot concatenate: lesson {} not found", lessonId);
            return false;
        }

        List<String> segmentUrls = lesson.getRecordingSegmentsList();
        if (segmentUrls.isEmpty()) {
            log.debug("No segments to concatenate for lesson {}", lessonId);
            return false;
        }

        if (segmentUrls.size() == 1) {
            // Only one segment, no concatenation needed — clear segments to prevent reprocessing
            log.debug("Only one segment for lesson {}, no concatenation needed", lessonId);
            lesson.setRecordingSegments(null);
            lessonRepository.save(lesson);
            return true;
        }

        log.info("Starting concatenation of {} segments for lesson {}", segmentUrls.size(), lessonId);

        // Create temp directory for this concatenation job
        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory(Path.of(TEMP_CONCAT_DIR), "concat-" + lessonId + "-");

            // Find local files for all segments
            List<File> segmentFiles = findLocalSegmentFiles(lessonId, segmentUrls.size());

            if (segmentFiles.isEmpty()) {
                log.warn("No local segment files found for lesson {}", lessonId);
                return false;
            }
            if (segmentFiles.size() < segmentUrls.size()) {
                log.warn("Not all local files found for lesson {} (found {}/{}) - proceeding with available segments",
                        lessonId, segmentFiles.size(), segmentUrls.size());
            }

            // Sort files by timestamp in filename to ensure correct order
            segmentFiles.sort((f1, f2) -> f1.getName().compareTo(f2.getName()));

            // Concatenate segments using FFmpeg
            File concatenatedFile = concatenateWithFFmpeg(segmentFiles, tempDir);

            if (concatenatedFile == null || !concatenatedFile.exists()) {
                log.error("FFmpeg concatenation failed for lesson {}", lessonId);
                return false;
            }

            // Upload concatenated video to Bunny CDN
            String finalUrl = uploadConcatenatedVideo(lesson, concatenatedFile);

            // If CDN upload fails, save locally and serve via nginx
            if (finalUrl == null) {
                log.warn("CDN upload failed for lesson {}, saving locally", lessonId);
                File localDir = new File(RECORDINGS_BASE_PATH, "concatenated");
                localDir.mkdirs();
                String localFilename = "lesson-" + lessonId + "-concatenated.mp4";
                File localDest = new File(localDir, localFilename);
                Files.copy(concatenatedFile.toPath(), localDest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                finalUrl = "https://meet.mychess.fr/recordings/concatenated/" + localFilename;
                log.info("Concatenated video saved locally: {}", finalUrl);
            }

            // Update lesson with final recording URL
            lesson.setRecordingUrl(finalUrl);
            // Clear segments JSON as they're no longer needed
            lesson.setRecordingSegments(null);
            lessonRepository.save(lesson);

            // Generate thumbnail from concatenated video
            thumbnailService.generateThumbnailFromLocalFile(lessonId, concatenatedFile);

            // Clean up: delete segment files (keep concatenated result)
            for (File segmentFile : segmentFiles) {
                try {
                    Files.deleteIfExists(segmentFile.toPath());
                    log.debug("Deleted segment file: {}", segmentFile.getPath());
                } catch (Exception e) {
                    log.warn("Could not delete segment file: {}", segmentFile.getPath());
                }
            }

            log.info("Successfully concatenated {} segments for lesson {} into: {}",
                    segmentFiles.size(), lessonId, finalUrl);
            return true;

        } catch (Exception e) {
            log.error("Error concatenating segments for lesson {}", lessonId, e);
            return false;
        } finally {
            // Clean up temp directory
            if (tempDir != null) {
                try {
                    Files.walk(tempDir)
                            .sorted((p1, p2) -> -p1.compareTo(p2)) // Delete files before directories
                            .forEach(path -> {
                                try {
                                    Files.deleteIfExists(path);
                                } catch (IOException e) {
                                    // Ignore cleanup errors
                                }
                            });
                } catch (Exception e) {
                    log.warn("Could not clean up temp directory: {}", tempDir);
                }
            }
        }
    }

    /**
     * Find all local segment files for a lesson.
     * Jibri uses UUID directories, so we scan ALL subdirectories and match
     * by the lesson ID in the MP4 filename (e.g., chessconnect-{id}-*.mp4 or mychess-lesson-{id}*.mp4).
     */
    private List<File> findLocalSegmentFiles(Long lessonId, int expectedCount) {
        List<File> segmentFiles = new ArrayList<>();
        File baseDir = new File(RECORDINGS_BASE_PATH);

        if (!baseDir.exists() || !baseDir.isDirectory()) {
            return segmentFiles;
        }

        // Patterns to match in filenames
        String chessconnectPrefix = "chessconnect-" + lessonId + "-";
        String mychessPrefix = "mychess-lesson-" + lessonId;

        // Scan ALL subdirectories (Jibri uses UUID directory names)
        File[] allDirs = baseDir.listFiles(File::isDirectory);
        if (allDirs == null) {
            return segmentFiles;
        }

        for (File directory : allDirs) {
            File[] mp4Files = directory.listFiles((dir, name) ->
                name.endsWith(".mp4") && (
                    name.startsWith(chessconnectPrefix) ||
                    name.startsWith(mychessPrefix)
                )
            );
            if (mp4Files != null) {
                for (File mp4File : mp4Files) {
                    segmentFiles.add(mp4File);
                }
            }
        }

        log.info("Found {} local segment files for lesson {} (expected {})",
                segmentFiles.size(), lessonId, expectedCount);
        return segmentFiles;
    }

    /**
     * Concatenate video files using FFmpeg.
     * Uses the concat demuxer for fast, lossless concatenation.
     */
    private File concatenateWithFFmpeg(List<File> segmentFiles, Path tempDir) throws IOException, InterruptedException {
        // Create concat list file for FFmpeg
        File concatListFile = tempDir.resolve("concat-list.txt").toFile();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(concatListFile))) {
            for (File segment : segmentFiles) {
                // FFmpeg concat format: file '/path/to/file.mp4'
                writer.write("file '" + segment.getAbsolutePath() + "'\n");
            }
        }

        // Output file
        File outputFile = tempDir.resolve("concatenated-" + UUID.randomUUID() + ".mp4").toFile();

        // Build FFmpeg command
        // Re-encode to fix frozen frames at segment boundaries (keyframe alignment issues)
        // Using libx264/aac ensures clean output even when segments have different parameters
        // Scale filter centers video and eliminates black bars from resolution mismatches
        ProcessBuilder pb = new ProcessBuilder(
            "ffmpeg",
            "-fflags", "+genpts",           // Regenerate presentation timestamps
            "-f", "concat",
            "-safe", "0",
            "-i", concatListFile.getAbsolutePath(),
            "-vf", "scale=1280:720:force_original_aspect_ratio=decrease,pad=1280:720:-1:-1:color=black",
            "-c:v", "libx264",             // Re-encode video to fix keyframe issues
            "-preset", "medium",            // Better quality, 2x slower than fast
            "-crf", "20",                   // Good quality (18=excellent, 23=medium, 28=bad)
            "-c:a", "aac",                  // Re-encode audio for compatibility
            "-b:a", "192k",                 // Better audio quality
            "-movflags", "+faststart",      // Enable progressive download
            "-y",                           // Overwrite output file
            outputFile.getAbsolutePath()
        );

        pb.redirectErrorStream(true);
        Process process = pb.start();

        // Log FFmpeg output
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.debug("FFmpeg: {}", line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            log.error("FFmpeg exited with code {}", exitCode);
            return null;
        }

        return outputFile;
    }

    /**
     * Upload concatenated video to Bunny CDN.
     */
    private String uploadConcatenatedVideo(Lesson lesson, File videoFile) {
        try {
            byte[] fileBytes = Files.readAllBytes(videoFile.toPath());

            // Try Bunny Storage first (preferred)
            if (bunnyStorageService.isConfigured()) {
                String cdnFilename = bunnyStorageService.generateRecordingFilename(lesson.getId());
                String cdnUrl = bunnyStorageService.uploadRecording(fileBytes, cdnFilename);
                if (cdnUrl != null) {
                    return cdnUrl;
                }
            }

            // Fall back to Bunny Stream
            if (bunnyStreamService.isConfigured()) {
                String title = "Cours " + lesson.getId();
                try {
                    title = "Cours " + lesson.getId() + " - " +
                            lesson.getTeacher().getFirstName() + " & " +
                            (lesson.getStudent() != null ? lesson.getStudent().getFirstName() : "Groupe");
                } catch (Exception e) {
                    // Lazy loading might fail, use simple title
                }
                return bunnyStreamService.uploadAndGetUrl(title, videoFile);
            }

            return null;
        } catch (Exception e) {
            log.error("Error uploading concatenated video for lesson {}", lesson.getId(), e);
            return null;
        }
    }

    /**
     * Check if FFmpeg is available on the system.
     */
    public boolean isFFmpegAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-version");
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            log.warn("FFmpeg not available: {}", e.getMessage());
            return false;
        }
    }
}
