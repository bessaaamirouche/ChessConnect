package com.chessconnect.dto.zoom;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ZoomMeetingResponse(
        Long id,
        String uuid,
        @JsonProperty("host_id")
        String hostId,
        @JsonProperty("host_email")
        String hostEmail,
        String topic,
        int type,
        String status,
        @JsonProperty("start_time")
        String startTime,
        int duration,
        String timezone,
        @JsonProperty("start_url")
        String startUrl,
        @JsonProperty("join_url")
        String joinUrl,
        String password
) {}
