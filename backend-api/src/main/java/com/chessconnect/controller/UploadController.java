package com.chessconnect.controller;

import com.chessconnect.model.User;
import com.chessconnect.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
public class UploadController {

    private final UserRepository userRepository;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.base-url:http://localhost:8282}")
    private String baseUrl;

    public UploadController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/avatar")
    public ResponseEntity<?> uploadAvatar(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("file") MultipartFile file
    ) {
        try {
            User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Le fichier est vide"));
            }

            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().body(Map.of("message", "Le fichier doit etre une image"));
            }

            // Limit file size (5MB)
            if (file.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity.badRequest().body(Map.of("message", "L'image ne doit pas depasser 5 Mo"));
            }

            // Create upload directory if not exists
            Path uploadPath = Paths.get(uploadDir, "avatars");
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Delete old avatar if exists
            String oldAvatarUrl = user.getAvatarUrl();
            if (oldAvatarUrl != null && oldAvatarUrl.contains("/uploads/avatars/")) {
                String oldFileName = oldAvatarUrl.substring(oldAvatarUrl.lastIndexOf("/") + 1);
                Path oldFilePath = uploadPath.resolve(oldFileName);
                Files.deleteIfExists(oldFilePath);
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String newFilename = UUID.randomUUID().toString() + extension;

            // Save file
            Path filePath = uploadPath.resolve(newFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Update user avatar URL
            String avatarUrl = baseUrl + "/uploads/avatars/" + newFilename;
            user.setAvatarUrl(avatarUrl);
            userRepository.save(user);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "avatarUrl", avatarUrl
            ));

        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "message", "Erreur lors de l'upload: " + e.getMessage()
            ));
        }
    }

    @DeleteMapping("/avatar")
    public ResponseEntity<?> deleteAvatar(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            String avatarUrl = user.getAvatarUrl();
            if (avatarUrl != null && avatarUrl.contains("/uploads/avatars/")) {
                String fileName = avatarUrl.substring(avatarUrl.lastIndexOf("/") + 1);
                Path filePath = Paths.get(uploadDir, "avatars", fileName);
                Files.deleteIfExists(filePath);
            }

            user.setAvatarUrl(null);
            userRepository.save(user);

            return ResponseEntity.ok(Map.of("success", true));

        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "message", "Erreur lors de la suppression: " + e.getMessage()
            ));
        }
    }
}
