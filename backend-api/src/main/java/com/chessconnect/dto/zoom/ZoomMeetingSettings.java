package com.chessconnect.dto.zoom;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ZoomMeetingSettings(
        @JsonProperty("host_video")
        boolean hostVideo,

        @JsonProperty("participant_video")
        boolean participantVideo,

        @JsonProperty("join_before_host")
        boolean joinBeforeHost,

        @JsonProperty("jbh_time")
        int jbhTime,

        @JsonProperty("mute_upon_entry")
        boolean muteUponEntry,

        @JsonProperty("waiting_room")
        boolean waitingRoom,

        String audio
) {}
