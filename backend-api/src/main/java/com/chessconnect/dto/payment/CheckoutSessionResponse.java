package com.chessconnect.dto.payment;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CheckoutSessionResponse {
    private String sessionId;
    private String url;
    private String publishableKey;
}
