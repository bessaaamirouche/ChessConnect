package com.chessconnect.dto.zoom;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ZoomMeetingRequest(
        String topic,
        int type,
        @JsonProperty("start_time")
        String startTime,
        int duration,
        String timezone,
        ZoomMeetingSettings settings
) {
    public static ZoomMeetingRequest forLesson(String teacherName, String studentName,
                                                String startTime, int durationMinutes) {
        return new ZoomMeetingRequest(
                "Cours d'échecs - " + teacherName + " / " + studentName,
                2, // Scheduled meeting
                startTime,
                durationMinutes,
                "Europe/Paris",
                new ZoomMeetingSettings(
                        true,  // host_video
                        true,  // participant_video
                        true,  // join_before_host
                        0,     // jbh_time (0 = anytime)
                        true,  // mute_upon_entry
                        true,  // waiting_room
                        "both" // audio
                )
        );
    }
}
