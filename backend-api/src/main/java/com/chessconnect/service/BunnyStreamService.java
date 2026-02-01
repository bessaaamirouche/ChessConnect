package com.chessconnect.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Duration;

@Service
public class BunnyStreamService {

    private static final Logger log = LoggerFactory.getLogger(BunnyStreamService.class);
    private static final String BUNNY_API_BASE = "https://video.bunnycdn.com/library";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${bunny.stream.api-key:}")
    private String apiKey;

    @Value("${bunny.stream.library-id:}")
    private String libraryId;

    @Value("${bunny.stream.cdn-hostname:}")
    private String cdnHostname;

    public BunnyStreamService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Check if Bunny Stream is configured
     */
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank()
                && libraryId != null && !libraryId.isBlank();
    }

    /**
     * Create a new video in the library and get upload URL
     * @param title Video title
     * @return Video GUID if successful, null otherwise
     */
    public String createVideo(String title) {
        if (!isConfigured()) {
            log.warn("Bunny Stream not configured");
            return null;
        }

        try {
            String url = BUNNY_API_BASE + "/" + libraryId + "/videos";
            String body = objectMapper.writeValueAsString(new CreateVideoRequest(title));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("AccessKey", apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 || response.statusCode() == 201) {
                JsonNode json = objectMapper.readTree(response.body());
                String guid = json.get("guid").asText();
                log.info("Created Bunny video with GUID: {}", guid);
                return guid;
            } else {
                log.error("Failed to create Bunny video: {} - {}", response.statusCode(), response.body());
                return null;
            }
        } catch (Exception e) {
            log.error("Error creating Bunny video", e);
            return null;
        }
    }

    /**
     * Upload a video file to Bunny Stream
     * @param videoGuid The video GUID from createVideo
     * @param videoFile The video file to upload
     * @return true if upload successful
     */
    public boolean uploadVideo(String videoGuid, File videoFile) {
        if (!isConfigured() || videoGuid == null || videoFile == null) {
            return false;
        }

        try {
            String url = BUNNY_API_BASE + "/" + libraryId + "/videos/" + videoGuid;

            byte[] fileBytes = Files.readAllBytes(videoFile.toPath());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("AccessKey", apiKey)
                    .header("Content-Type", "application/octet-stream")
                    .PUT(HttpRequest.BodyPublishers.ofByteArray(fileBytes))
                    .timeout(Duration.ofMinutes(30))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 || response.statusCode() == 201) {
                log.info("Successfully uploaded video {} to Bunny Stream", videoGuid);
                return true;
            } else {
                log.error("Failed to upload video to Bunny: {} - {}", response.statusCode(), response.body());
                return false;
            }
        } catch (Exception e) {
            log.error("Error uploading video to Bunny", e);
            return false;
        }
    }

    /**
     * Get the video status from Bunny Stream
     * @param videoGuid The video GUID
     * @return Video status (0=created, 1=uploaded, 2=processing, 3=transcoding, 4=finished, 5=error)
     */
    public Integer getVideoStatus(String videoGuid) {
        if (!isConfigured() || videoGuid == null) {
            return null;
        }

        try {
            String url = BUNNY_API_BASE + "/" + libraryId + "/videos/" + videoGuid;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("AccessKey", apiKey)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode json = objectMapper.readTree(response.body());
                return json.get("status").asInt();
            }
            return null;
        } catch (Exception e) {
            log.error("Error getting video status from Bunny", e);
            return null;
        }
    }

    /**
     * Get the playback URL for a video
     * @param videoGuid The video GUID
     * @return The CDN playback URL
     */
    public String getPlaybackUrl(String videoGuid) {
        if (cdnHostname == null || cdnHostname.isBlank() || videoGuid == null) {
            return null;
        }
        return "https://" + cdnHostname + "/" + videoGuid + "/playlist.m3u8";
    }

    /**
     * Get the direct MP4 playback URL for a video
     * @param videoGuid The video GUID
     * @return The CDN direct MP4 URL
     */
    public String getDirectPlaybackUrl(String videoGuid) {
        if (cdnHostname == null || cdnHostname.isBlank() || videoGuid == null) {
            return null;
        }
        return "https://" + cdnHostname + "/" + videoGuid + "/play_720p.mp4";
    }

    /**
     * Delete a video from Bunny Stream
     * @param videoGuid The video GUID
     * @return true if deletion successful
     */
    public boolean deleteVideo(String videoGuid) {
        if (!isConfigured() || videoGuid == null) {
            return false;
        }

        try {
            String url = BUNNY_API_BASE + "/" + libraryId + "/videos/" + videoGuid;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("AccessKey", apiKey)
                    .DELETE()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 || response.statusCode() == 204) {
                log.info("Successfully deleted video {} from Bunny Stream", videoGuid);
                return true;
            } else {
                log.error("Failed to delete video from Bunny: {} - {}", response.statusCode(), response.body());
                return false;
            }
        } catch (Exception e) {
            log.error("Error deleting video from Bunny", e);
            return false;
        }
    }

    /**
     * Upload a video file and return the playback URL
     * This is a convenience method that creates the video entry and uploads in one call
     * @param title Video title
     * @param videoFile The video file to upload
     * @return The playback URL if successful, null otherwise
     */
    public String uploadAndGetUrl(String title, File videoFile) {
        String guid = createVideo(title);
        if (guid == null) {
            return null;
        }

        boolean uploaded = uploadVideo(guid, videoFile);
        if (!uploaded) {
            // Clean up the created video entry
            deleteVideo(guid);
            return null;
        }

        return getPlaybackUrl(guid);
    }

    // Inner class for create video request
    private static class CreateVideoRequest {
        public String title;

        public CreateVideoRequest(String title) {
            this.title = title;
        }
    }
}
