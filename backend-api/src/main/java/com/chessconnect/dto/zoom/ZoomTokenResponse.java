package com.chessconnect.dto.zoom;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ZoomTokenResponse(
        @JsonProperty("access_token")
        String accessToken,

        @JsonProperty("token_type")
        String tokenType,

        @JsonProperty("expires_in")
        int expiresIn,

        String scope
) {}
