package com.chessconnect.config;

import nl.martijndwars.webpush.PushService;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.interfaces.ECPrivateKey;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.security.*;
import java.util.Base64;

/**
 * Configuration for Web Push notifications using VAPID keys.
 *
 * VAPID keys can be provided via environment variables or generated automatically.
 * If keys are not provided, they will be generated at startup and logged for configuration.
 */
@Configuration
public class WebPushConfig {

    private static final Logger log = LoggerFactory.getLogger(WebPushConfig.class);

    @Value("${VAPID_PUBLIC_KEY:}")
    private String vapidPublicKey;

    @Value("${VAPID_PRIVATE_KEY:}")
    private String vapidPrivateKey;

    @Value("${VAPID_SUBJECT:mailto:contact@mychess.fr}")
    private String vapidSubject;

    private String effectivePublicKey;
    private String effectivePrivateKey;

    @PostConstruct
    public void init() {
        // Register BouncyCastle provider
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }

        // Use provided keys or generate new ones
        if (vapidPublicKey != null && !vapidPublicKey.isEmpty() &&
            vapidPrivateKey != null && !vapidPrivateKey.isEmpty()) {
            effectivePublicKey = vapidPublicKey;
            effectivePrivateKey = vapidPrivateKey;
            log.info("Using configured VAPID keys");
        } else {
            try {
                KeyPair keyPair = generateVapidKeyPair();
                effectivePublicKey = encodePublicKey((ECPublicKey) keyPair.getPublic());
                effectivePrivateKey = encodePrivateKey((ECPrivateKey) keyPair.getPrivate());

                log.warn("=================================================================");
                log.warn("VAPID keys not configured! Generated new keys for this session.");
                log.warn("For production, add these to your .env file:");
                log.warn("VAPID_PUBLIC_KEY={}", effectivePublicKey);
                log.warn("VAPID_PRIVATE_KEY={}", effectivePrivateKey);
                log.warn("=================================================================");
            } catch (GeneralSecurityException e) {
                log.error("Failed to generate VAPID keys", e);
                throw new RuntimeException("Failed to initialize Web Push configuration", e);
            }
        }
    }

    /**
     * Generate a VAPID key pair using the P-256 curve.
     */
    private KeyPair generateVapidKeyPair() throws GeneralSecurityException {
        ECNamedCurveParameterSpec parameterSpec = ECNamedCurveTable.getParameterSpec("prime256v1");
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME);
        keyPairGenerator.initialize(parameterSpec);
        return keyPairGenerator.generateKeyPair();
    }

    /**
     * Encode the public key to URL-safe Base64.
     */
    private String encodePublicKey(ECPublicKey publicKey) {
        byte[] encoded = publicKey.getQ().getEncoded(false);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(encoded);
    }

    /**
     * Encode the private key to URL-safe Base64.
     */
    private String encodePrivateKey(ECPrivateKey privateKey) {
        byte[] encoded = privateKey.getD().toByteArray();
        // Remove leading zero if present (for 32-byte alignment)
        if (encoded.length == 33 && encoded[0] == 0) {
            byte[] tmp = new byte[32];
            System.arraycopy(encoded, 1, tmp, 0, 32);
            encoded = tmp;
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(encoded);
    }

    @Bean
    public PushService pushService() throws GeneralSecurityException {
        PushService pushService = new PushService();
        pushService.setPublicKey(effectivePublicKey);
        pushService.setPrivateKey(effectivePrivateKey);
        pushService.setSubject(vapidSubject);
        return pushService;
    }

    /**
     * Get the public VAPID key for client subscription.
     */
    public String getPublicKey() {
        return effectivePublicKey;
    }
}
