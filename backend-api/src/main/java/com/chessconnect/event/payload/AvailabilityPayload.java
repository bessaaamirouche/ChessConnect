package com.chessconnect.event.payload;

/**
 * Payload for new availability events.
 */
public record AvailabilityPayload(
    Long availabilityId,
    Long teacherId,
    String teacherName,
    String dayInfo,
    String timeRange
) {}
