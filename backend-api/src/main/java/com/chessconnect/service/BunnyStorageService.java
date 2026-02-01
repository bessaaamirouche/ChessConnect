package com.chessconnect.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Duration;

/**
 * Service for uploading and managing files on Bunny CDN Storage.
 * This is for direct file storage (not video streaming with transcoding).
 *
 * API Reference: https://docs.bunny.net/reference/storage-api
 */
@Service
public class BunnyStorageService {

    private static final Logger log = LoggerFactory.getLogger(BunnyStorageService.class);
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 2000; // 2 seconds initial delay

    private final HttpClient httpClient;

    @Value("${bunny.storage.zone:}")
    private String storageZone;

    @Value("${bunny.storage.api-key:}")
    private String apiKey;

    @Value("${bunny.storage.host:storage.bunnycdn.com}")
    private String storageHost;

    @Value("${bunny.cdn.url:}")
    private String cdnUrl;

    public BunnyStorageService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    /**
     * Check if Bunny Storage is configured
     */
    public boolean isConfigured() {
        return storageZone != null && !storageZone.isBlank()
                && apiKey != null && !apiKey.isBlank()
                && cdnUrl != null && !cdnUrl.isBlank();
    }

    /**
     * Upload a recording file to Bunny Storage from byte array.
     *
     * @param fileContent The file content as byte array
     * @param filename The filename to use on storage (e.g., "lesson-123-1706745600.mp4")
     * @return The CDN URL if successful, null otherwise
     */
    public String uploadRecording(byte[] fileContent, String filename) {
        if (!isConfigured()) {
            log.warn("Bunny Storage not configured, cannot upload recording");
            return null;
        }

        if (fileContent == null || fileContent.length == 0) {
            log.warn("Cannot upload empty file");
            return null;
        }

        String sanitizedFilename = sanitizeFilename(filename);
        String uploadUrl = buildStorageUrl(sanitizedFilename);

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(uploadUrl))
                        .header("AccessKey", apiKey)
                        .header("Content-Type", "application/octet-stream")
                        .PUT(HttpRequest.BodyPublishers.ofByteArray(fileContent))
                        .timeout(Duration.ofMinutes(5))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 201 || response.statusCode() == 200) {
                    String cdnFileUrl = buildCdnUrl(sanitizedFilename);
                    log.info("Successfully uploaded recording to Bunny CDN: {}", cdnFileUrl);
                    return cdnFileUrl;
                } else {
                    log.warn("Attempt {}/{}: Failed to upload to Bunny Storage: {} - {}",
                            attempt, MAX_RETRIES, response.statusCode(), response.body());
                }
            } catch (Exception e) {
                log.warn("Attempt {}/{}: Error uploading to Bunny Storage: {}",
                        attempt, MAX_RETRIES, e.getMessage());
            }

            if (attempt < MAX_RETRIES) {
                try {
                    long delay = RETRY_DELAY_MS * (long) Math.pow(2, attempt - 1);
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        log.error("Failed to upload recording after {} attempts", MAX_RETRIES);
        return null;
    }

    /**
     * Upload a recording file to Bunny Storage from InputStream.
     * Suitable for large files that shouldn't be loaded entirely into memory.
     *
     * @param stream The input stream of the file
     * @param filename The filename to use on storage
     * @param contentLength The content length of the file
     * @return The CDN URL if successful, null otherwise
     */
    public String uploadRecording(InputStream stream, String filename, long contentLength) {
        if (!isConfigured()) {
            log.warn("Bunny Storage not configured, cannot upload recording");
            return null;
        }

        if (stream == null || contentLength <= 0) {
            log.warn("Cannot upload empty or invalid stream");
            return null;
        }

        String sanitizedFilename = sanitizeFilename(filename);
        String uploadUrl = buildStorageUrl(sanitizedFilename);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(uploadUrl))
                    .header("AccessKey", apiKey)
                    .header("Content-Type", "application/octet-stream")
                    .header("Content-Length", String.valueOf(contentLength))
                    .PUT(HttpRequest.BodyPublishers.ofInputStream(() -> stream))
                    .timeout(Duration.ofMinutes(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 201 || response.statusCode() == 200) {
                String cdnFileUrl = buildCdnUrl(sanitizedFilename);
                log.info("Successfully uploaded recording stream to Bunny CDN: {}", cdnFileUrl);
                return cdnFileUrl;
            } else {
                log.error("Failed to upload stream to Bunny Storage: {} - {}",
                        response.statusCode(), response.body());
                return null;
            }
        } catch (Exception e) {
            log.error("Error uploading stream to Bunny Storage", e);
            return null;
        }
    }

    /**
     * Upload a recording file to Bunny Storage from a File object.
     *
     * @param file The file to upload
     * @param filename The filename to use on storage (if null, uses the file's name)
     * @return The CDN URL if successful, null otherwise
     */
    public String uploadRecording(File file, String filename) {
        if (!isConfigured()) {
            log.warn("Bunny Storage not configured, cannot upload recording");
            return null;
        }

        if (file == null || !file.exists() || !file.canRead()) {
            log.warn("Cannot upload: file does not exist or is not readable");
            return null;
        }

        String targetFilename = filename != null ? filename : file.getName();

        try {
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            return uploadRecording(fileBytes, targetFilename);
        } catch (IOException e) {
            log.error("Error reading file for upload: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Delete a recording from Bunny Storage.
     *
     * @param filename The filename to delete
     * @return true if deletion was successful
     */
    public boolean deleteRecording(String filename) {
        if (!isConfigured()) {
            log.warn("Bunny Storage not configured, cannot delete recording");
            return false;
        }

        if (filename == null || filename.isBlank()) {
            log.warn("Cannot delete: filename is empty");
            return false;
        }

        String sanitizedFilename = sanitizeFilename(filename);
        String deleteUrl = buildStorageUrl(sanitizedFilename);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(deleteUrl))
                    .header("AccessKey", apiKey)
                    .DELETE()
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 || response.statusCode() == 204) {
                log.info("Successfully deleted recording from Bunny CDN: {}", sanitizedFilename);
                return true;
            } else if (response.statusCode() == 404) {
                log.debug("Recording not found on Bunny CDN (already deleted?): {}", sanitizedFilename);
                return true; // Consider it successful if file doesn't exist
            } else {
                log.error("Failed to delete from Bunny Storage: {} - {}",
                        response.statusCode(), response.body());
                return false;
            }
        } catch (Exception e) {
            log.error("Error deleting from Bunny Storage", e);
            return false;
        }
    }

    /**
     * Extract filename from a CDN URL.
     *
     * @param cdnUrl The CDN URL (e.g., "https://mychess.b-cdn.net/lesson-123-1706745600.mp4")
     * @return The filename, or null if extraction fails
     */
    public String extractFilenameFromUrl(String cdnUrl) {
        if (cdnUrl == null || cdnUrl.isBlank()) {
            return null;
        }

        try {
            URI uri = URI.create(cdnUrl);
            String path = uri.getPath();
            if (path != null && path.length() > 1) {
                // Remove leading slash
                return path.startsWith("/") ? path.substring(1) : path;
            }
        } catch (Exception e) {
            log.warn("Failed to extract filename from URL: {}", cdnUrl);
        }
        return null;
    }

    /**
     * Check if a URL is from our Bunny CDN.
     *
     * @param url The URL to check
     * @return true if URL is from Bunny CDN
     */
    public boolean isBunnyCdnUrl(String url) {
        if (url == null || url.isBlank() || cdnUrl == null || cdnUrl.isBlank()) {
            return false;
        }
        return url.contains("b-cdn.net") || url.startsWith(cdnUrl);
    }

    /**
     * Generate a unique filename for a lesson recording.
     *
     * @param lessonId The lesson ID
     * @return A unique filename like "lesson-123-1706745600.mp4"
     */
    public String generateRecordingFilename(Long lessonId) {
        long timestamp = System.currentTimeMillis() / 1000;
        return String.format("lesson-%d-%d.mp4", lessonId, timestamp);
    }

    /**
     * Build the storage URL for uploading/deleting files.
     */
    private String buildStorageUrl(String filename) {
        return String.format("https://%s/%s/%s", storageHost, storageZone, filename);
    }

    /**
     * Build the CDN URL for accessing files.
     */
    private String buildCdnUrl(String filename) {
        String baseUrl = cdnUrl.endsWith("/") ? cdnUrl.substring(0, cdnUrl.length() - 1) : cdnUrl;
        return String.format("%s/%s", baseUrl, filename);
    }

    /**
     * Sanitize filename to prevent path traversal and invalid characters.
     */
    private String sanitizeFilename(String filename) {
        if (filename == null) {
            return "unknown.mp4";
        }
        // Remove path components and invalid characters
        String sanitized = filename.replaceAll("[^a-zA-Z0-9._-]", "_");
        // Ensure it ends with .mp4
        if (!sanitized.toLowerCase().endsWith(".mp4")) {
            sanitized += ".mp4";
        }
        return sanitized;
    }
}
