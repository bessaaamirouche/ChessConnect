package com.chessconnect.event;

import com.chessconnect.service.SseConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listens for NotificationEvents and dispatches them to connected SSE clients.
 */
@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final SseConnectionManager sseConnectionManager;

    public NotificationEventListener(SseConnectionManager sseConnectionManager) {
        this.sseConnectionManager = sseConnectionManager;
    }

    @EventListener
    @Async("sseTaskExecutor")
    public void handleNotificationEvent(NotificationEvent event) {
        String eventName = mapEventTypeToName(event.getEventType());
        Long userId = event.getTargetUserId();

        if (userId != null) {
            // Send to specific user
            sseConnectionManager.sendToUser(userId, eventName, event.getPayload());
            log.debug("Dispatched {} event to user {}", eventName, userId);
        }
    }

    private String mapEventTypeToName(NotificationEvent.EventType type) {
        return switch (type) {
            case NOTIFICATION_CREATED -> "notification";
            case LESSON_STATUS_CHANGED -> "lesson_status";
            case LESSON_BOOKED -> "lesson_booked";
            case AVAILABILITY_CREATED -> "availability";
            case TEACHER_JOINED_CALL -> "teacher_joined";
        };
    }
}
