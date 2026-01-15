package com.chessconnect.service.zoom;

import com.chessconnect.config.ZoomConfig;
import com.chessconnect.dto.zoom.ZoomMeetingRequest;
import com.chessconnect.dto.zoom.ZoomMeetingResponse;
import com.chessconnect.dto.zoom.ZoomTokenResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@Service
public class ZoomService {

    private static final Logger log = LoggerFactory.getLogger(ZoomService.class);
    private static final DateTimeFormatter ZOOM_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final ZoomConfig zoomConfig;
    private final WebClient webClient;

    private String cachedToken;
    private Instant tokenExpiry;

    public ZoomService(ZoomConfig zoomConfig, WebClient zoomWebClient) {
        this.zoomConfig = zoomConfig;
        this.webClient = zoomWebClient;
    }

    public ZoomMeetingResponse createMeeting(String teacherName, String studentName,
                                              LocalDateTime scheduledAt, int durationMinutes) {
        String accessToken = getAccessToken();

        String startTime = scheduledAt.atZone(ZoneId.of("Europe/Paris"))
                .format(ZOOM_DATE_FORMAT);

        ZoomMeetingRequest request = ZoomMeetingRequest.forLesson(
                teacherName, studentName, startTime, durationMinutes
        );

        try {
            ZoomMeetingResponse response = webClient.post()
                    .uri("/users/me/meetings")
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(ZoomMeetingResponse.class)
                    .block();

            log.info("Created Zoom meeting: {} for {} / {}",
                    response.id(), teacherName, studentName);

            return response;
        } catch (Exception e) {
            log.error("Failed to create Zoom meeting", e);
            throw new RuntimeException("Unable to create Zoom meeting: " + e.getMessage());
        }
    }

    public void deleteMeeting(String meetingId) {
        String accessToken = getAccessToken();

        try {
            webClient.delete()
                    .uri("/meetings/{meetingId}", meetingId)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            log.info("Deleted Zoom meeting: {}", meetingId);
        } catch (Exception e) {
            log.warn("Failed to delete Zoom meeting {}: {}", meetingId, e.getMessage());
        }
    }

    private String getAccessToken() {
        if (cachedToken != null && tokenExpiry != null && Instant.now().isBefore(tokenExpiry)) {
            return cachedToken;
        }

        String credentials = zoomConfig.getClientId() + ":" + zoomConfig.getClientSecret();
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());

        try {
            ZoomTokenResponse response = WebClient.create(zoomConfig.getOauthUrl())
                    .post()
                    .header("Authorization", "Basic " + encodedCredentials)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData("grant_type", "account_credentials")
                            .with("account_id", zoomConfig.getAccountId()))
                    .retrieve()
                    .bodyToMono(ZoomTokenResponse.class)
                    .block();

            if (response != null) {
                cachedToken = response.accessToken();
                tokenExpiry = Instant.now().plusSeconds(response.expiresIn() - 60);
                log.debug("Obtained new Zoom access token, expires in {} seconds", response.expiresIn());
                return cachedToken;
            }

            throw new RuntimeException("Empty token response from Zoom");
        } catch (Exception e) {
            log.error("Failed to obtain Zoom access token", e);
            throw new RuntimeException("Unable to authenticate with Zoom: " + e.getMessage());
        }
    }
}
