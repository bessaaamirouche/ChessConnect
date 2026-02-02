package com.chessconnect.event;

import org.springframework.context.ApplicationEvent;

/**
 * Event class for SSE notifications.
 * Published by services when changes occur that should be pushed to connected clients.
 */
public class NotificationEvent extends ApplicationEvent {

    public enum EventType {
        NOTIFICATION_CREATED,     // Backend notification (refund, lesson confirmed, etc.)
        LESSON_STATUS_CHANGED,    // Lesson PENDING -> CONFIRMED, CANCELLED, etc.
        LESSON_BOOKED,            // New lesson booking (for teachers)
        AVAILABILITY_CREATED,     // Teacher added availability (for subscribed students)
        TEACHER_JOINED_CALL       // Teacher joined video call (for students)
    }

    private final EventType eventType;
    private final Long targetUserId;
    private final Object payload;

    public NotificationEvent(Object source, EventType eventType, Long targetUserId, Object payload) {
        super(source);
        this.eventType = eventType;
        this.targetUserId = targetUserId;
        this.payload = payload;
    }

    public EventType getEventType() {
        return eventType;
    }

    public Long getTargetUserId() {
        return targetUserId;
    }

    public Object getPayload() {
        return payload;
    }
}
