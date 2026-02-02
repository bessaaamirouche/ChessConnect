package com.chessconnect.event.payload;

/**
 * Payload for backend notifications (refunds, confirmations, etc.).
 */
public record BackendNotificationPayload(
    Long notificationId,
    String type,
    String title,
    String message,
    String link,
    String createdAt
) {}
