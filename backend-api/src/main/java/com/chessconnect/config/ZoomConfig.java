package com.chessconnect.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@ConfigurationProperties(prefix = "zoom")
public class ZoomConfig {

    private String accountId;
    private String clientId;
    private String clientSecret;
    private String baseUrl;
    private String oauthUrl;

    @Bean
    public WebClient zoomWebClient() {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    // Getters and Setters
    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getClientSecret() { return clientSecret; }
    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getOauthUrl() { return oauthUrl; }
    public void setOauthUrl(String oauthUrl) { this.oauthUrl = oauthUrl; }
}
