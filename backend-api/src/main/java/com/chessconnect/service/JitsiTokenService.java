package com.chessconnect.service;

import com.chessconnect.model.User;
import com.chessconnect.model.enums.UserRole;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JitsiTokenService {

    @Value("${jitsi.app-id:mychess}")
    private String appId;

    @Value("${jitsi.app-secret:cb2431b0b8102427d94c0365abc301e8b9714d5d5eca6526e115de638f8ae77d}")
    private String appSecret;

    @Value("${jitsi.domain:meet.mychess.fr}")
    private String jitsiDomain;

    public String generateToken(User user, String roomName) {
        // Utiliser seulement les 32 premiers caractères pour garantir HS256
        String secret256 = appSecret.length() > 32 ? appSecret.substring(0, 32) : appSecret;
        SecretKey key = Keys.hmacShaKeyFor(secret256.getBytes(StandardCharsets.UTF_8));

        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(3600); // Token valide 1 heure

        // Le prof est toujours moderateur
        boolean isModerator = user.getRole() == UserRole.TEACHER || user.getRole() == UserRole.ADMIN;

        // Context utilisateur pour Jitsi
        Map<String, Object> userContext = new HashMap<>();
        userContext.put("name", user.getFirstName() + " " + user.getLastName());
        userContext.put("email", user.getEmail());
        userContext.put("id", user.getId().toString());
        userContext.put("moderator", isModerator);

        Map<String, Object> context = new HashMap<>();
        context.put("user", userContext);

        // Features autorisees
        Map<String, Object> features = new HashMap<>();
        features.put("recording", isModerator); // Seul le moderateur peut enregistrer
        features.put("livestreaming", false);
        features.put("transcription", false);
        features.put("outbound-call", false);
        context.put("features", features);

        return Jwts.builder()
                .header()
                    .add("typ", "JWT")
                    .add("alg", "HS256")
                .and()
                .claim("context", context)
                .claim("aud", appId)
                .claim("iss", appId)
                .claim("sub", jitsiDomain)
                .claim("room", roomName)
                .claim("moderator", isModerator)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key)
                .compact();
    }
}
