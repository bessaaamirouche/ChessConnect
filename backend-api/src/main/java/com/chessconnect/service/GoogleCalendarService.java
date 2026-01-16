package com.chessconnect.service;

import com.chessconnect.config.GoogleCalendarConfig;
import com.chessconnect.model.Lesson;
import com.chessconnect.model.User;
import com.chessconnect.repository.UserRepository;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class GoogleCalendarService {

    private static final Logger log = LoggerFactory.getLogger(GoogleCalendarService.class);
    private static final String APPLICATION_NAME = "ChessConnect";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList(CalendarScopes.CALENDAR_EVENTS);

    private final GoogleCalendarConfig config;
    private final UserRepository userRepository;
    private NetHttpTransport httpTransport;

    public GoogleCalendarService(GoogleCalendarConfig config, UserRepository userRepository) {
        this.config = config;
        this.userRepository = userRepository;
        try {
            this.httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        } catch (GeneralSecurityException | IOException e) {
            log.error("Failed to initialize HTTP transport", e);
        }
    }

    /**
     * Generate the Google OAuth authorization URL
     */
    public String getAuthorizationUrl() {
        if (!config.isConfigured()) {
            throw new IllegalStateException("Google Calendar is not configured");
        }

        GoogleAuthorizationCodeFlow flow = buildFlow();
        return flow.newAuthorizationUrl()
                .setRedirectUri(config.getRedirectUri())
                .setAccessType("offline")
                .setApprovalPrompt("force")
                .build();
    }

    /**
     * Exchange authorization code for tokens and save them
     */
    @Transactional
    public void handleCallback(String code, User user) throws IOException {
        if (!config.isConfigured()) {
            throw new IllegalStateException("Google Calendar is not configured");
        }

        TokenResponse tokenResponse = new GoogleAuthorizationCodeTokenRequest(
                httpTransport, JSON_FACTORY,
                config.getClientId(), config.getClientSecret(),
                code, config.getRedirectUri()
        ).execute();

        user.setGoogleCalendarToken(tokenResponse.getAccessToken());
        user.setGoogleCalendarRefreshToken(tokenResponse.getRefreshToken());
        user.setGoogleCalendarEnabled(true);
        userRepository.save(user);

        log.info("Google Calendar connected for user {}", user.getId());
    }

    /**
     * Disconnect Google Calendar for a user
     */
    @Transactional
    public void disconnect(User user) {
        user.setGoogleCalendarToken(null);
        user.setGoogleCalendarRefreshToken(null);
        user.setGoogleCalendarEnabled(false);
        userRepository.save(user);
        log.info("Google Calendar disconnected for user {}", user.getId());
    }

    /**
     * Create a calendar event for a lesson
     */
    public String createLessonEvent(Lesson lesson, User user) {
        if (!isConnected(user) || !config.isConfigured()) {
            return null;
        }

        try {
            Calendar service = getCalendarService(user);
            if (service == null) return null;

            Event event = buildLessonEvent(lesson, user);
            Event createdEvent = service.events().insert("primary", event).execute();

            log.info("Created calendar event {} for lesson {}", createdEvent.getId(), lesson.getId());
            return createdEvent.getId();

        } catch (IOException e) {
            log.error("Failed to create calendar event for lesson {}", lesson.getId(), e);
            return null;
        }
    }

    /**
     * Delete a calendar event
     */
    public void deleteLessonEvent(String eventId, User user) {
        if (!isConnected(user) || eventId == null || !config.isConfigured()) {
            return;
        }

        try {
            Calendar service = getCalendarService(user);
            if (service == null) return;

            service.events().delete("primary", eventId).execute();
            log.info("Deleted calendar event {}", eventId);

        } catch (IOException e) {
            log.error("Failed to delete calendar event {}", eventId, e);
        }
    }

    /**
     * Check if Google Calendar is connected for a user
     */
    public boolean isConnected(User user) {
        return Boolean.TRUE.equals(user.getGoogleCalendarEnabled())
                && user.getGoogleCalendarToken() != null;
    }

    /**
     * Check if Google Calendar is configured on the server
     */
    public boolean isConfigured() {
        return config.isConfigured();
    }

    private GoogleAuthorizationCodeFlow buildFlow() {
        try {
            return new GoogleAuthorizationCodeFlow.Builder(
                    httpTransport, JSON_FACTORY,
                    config.getClientId(), config.getClientSecret(), SCOPES
            ).setAccessType("offline").build();
        } catch (Exception e) {
            log.error("Failed to build authorization flow", e);
            return null;
        }
    }

    private Calendar getCalendarService(User user) {
        try {
            GoogleAuthorizationCodeFlow flow = buildFlow();
            if (flow == null) return null;

            Credential credential = flow.loadCredential(user.getId().toString());
            if (credential == null) {
                // Create credential from stored tokens
                TokenResponse tokenResponse = new TokenResponse();
                tokenResponse.setAccessToken(user.getGoogleCalendarToken());
                tokenResponse.setRefreshToken(user.getGoogleCalendarRefreshToken());

                credential = flow.createAndStoreCredential(tokenResponse, user.getId().toString());
            }

            return new Calendar.Builder(httpTransport, JSON_FACTORY, credential)
                    .setApplicationName(APPLICATION_NAME)
                    .build();

        } catch (IOException e) {
            log.error("Failed to create calendar service for user {}", user.getId(), e);
            return null;
        }
    }

    private Event buildLessonEvent(Lesson lesson, User user) {
        User otherParty = user.getId().equals(lesson.getStudent().getId())
                ? lesson.getTeacher()
                : lesson.getStudent();

        String title = String.format("Cours d'echecs avec %s %s",
                otherParty.getFirstName(), otherParty.getLastName());

        Event event = new Event()
                .setSummary(title)
                .setDescription("Cours d'echecs via ChessConnect");

        ZonedDateTime startZoned = lesson.getScheduledAt().atZone(ZoneId.of("Europe/Paris"));
        ZonedDateTime endZoned = startZoned.plusMinutes(lesson.getDurationMinutes());

        EventDateTime start = new EventDateTime()
                .setDateTime(new DateTime(startZoned.toInstant().toEpochMilli()))
                .setTimeZone("Europe/Paris");

        EventDateTime end = new EventDateTime()
                .setDateTime(new DateTime(endZoned.toInstant().toEpochMilli()))
                .setTimeZone("Europe/Paris");

        event.setStart(start);
        event.setEnd(end);

        // Add Zoom link if available
        if (lesson.getZoomLink() != null) {
            event.setDescription("Cours d'echecs via ChessConnect\n\nLien: " + lesson.getZoomLink());
        }

        return event;
    }
}
