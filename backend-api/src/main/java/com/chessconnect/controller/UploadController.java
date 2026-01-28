package com.chessconnect.controller;

import com.chessconnect.model.User;
import com.chessconnect.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/upload")
public class UploadController {

    private static final Logger log = LoggerFactory.getLogger(UploadController.class);

    // Maximum file size: 5MB
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    // Allowed MIME types for avatars
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );

    // Allowed file extensions
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".webp"
    );

    // Magic numbers (file signatures) for image validation
    private static final byte[] JPEG_MAGIC = new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF };
    private static final byte[] PNG_MAGIC = new byte[] { (byte) 0x89, 0x50, 0x4E, 0x47 };
    private static final byte[] GIF87_MAGIC = new byte[] { 0x47, 0x49, 0x46, 0x38, 0x37, 0x61 };
    private static final byte[] GIF89_MAGIC = new byte[] { 0x47, 0x49, 0x46, 0x38, 0x39, 0x61 };
    private static final byte[] WEBP_MAGIC = new byte[] { 0x52, 0x49, 0x46, 0x46 }; // RIFF header

    private final UserRepository userRepository;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

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

            // Validate file is not empty
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Le fichier est vide"));
            }

            // Validate file size
            if (file.getSize() > MAX_FILE_SIZE) {
                return ResponseEntity.badRequest().body(Map.of("message", "L'image ne doit pas depasser 5 Mo"));
            }

            // Validate MIME type (Content-Type header)
            String contentType = file.getContentType();
            if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
                log.warn("Upload rejected - invalid MIME type: {} for user {}", contentType, user.getId());
                return ResponseEntity.badRequest().body(Map.of("message", "Type de fichier non autorise. Formats acceptes: JPEG, PNG, GIF, WebP"));
            }

            // Validate file extension
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
                log.warn("Upload rejected - invalid extension: {} for user {}", extension, user.getId());
                return ResponseEntity.badRequest().body(Map.of("message", "Extension de fichier non autorisee"));
            }

            // Validate magic number (file signature) - critical security check
            if (!isValidImageMagicNumber(file)) {
                log.warn("Upload rejected - invalid magic number for user {} (possible malicious file)", user.getId());
                return ResponseEntity.badRequest().body(Map.of("message", "Le fichier ne semble pas etre une image valide"));
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
                // Validate filename to prevent path traversal
                if (isValidFilename(oldFileName)) {
                    Path oldFilePath = uploadPath.resolve(oldFileName);
                    Files.deleteIfExists(oldFilePath);
                }
            }

            // Generate unique filename with sanitized extension
            String safeExtension = extension.toLowerCase();
            String newFilename = UUID.randomUUID().toString() + safeExtension;

            // Save file
            Path filePath = uploadPath.resolve(newFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Update user avatar URL (relative path, served by nginx)
            String avatarUrl = "/api/uploads/avatars/" + newFilename;
            user.setAvatarUrl(avatarUrl);
            userRepository.save(user);

            log.info("Avatar uploaded successfully for user {}", user.getId());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "avatarUrl", avatarUrl
            ));

        } catch (IOException e) {
            log.error("Error uploading avatar", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "message", "Erreur lors de l'upload du fichier"
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
                // Validate filename to prevent path traversal attacks
                if (isValidFilename(fileName)) {
                    Path filePath = Paths.get(uploadDir, "avatars", fileName);
                    Files.deleteIfExists(filePath);
                }
            }

            user.setAvatarUrl(null);
            userRepository.save(user);

            log.info("Avatar deleted for user {}", user.getId());

            return ResponseEntity.ok(Map.of("success", true));

        } catch (IOException e) {
            log.error("Error deleting avatar", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "message", "Erreur lors de la suppression du fichier"
            ));
        }
    }

    /**
     * Validate file by checking magic number (file signature).
     * This prevents attackers from uploading malicious files with spoofed extensions.
     */
    private boolean isValidImageMagicNumber(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[12]; // Read enough bytes for all signatures
            int bytesRead = is.read(header);
            if (bytesRead < 3) return false;

            // Check JPEG (FF D8 FF)
            if (startsWith(header, JPEG_MAGIC)) return true;

            // Check PNG (89 50 4E 47)
            if (startsWith(header, PNG_MAGIC)) return true;

            // Check GIF87a
            if (bytesRead >= 6 && startsWith(header, GIF87_MAGIC)) return true;

            // Check GIF89a
            if (bytesRead >= 6 && startsWith(header, GIF89_MAGIC)) return true;

            // Check WebP (RIFF....WEBP)
            if (bytesRead >= 12 && startsWith(header, WEBP_MAGIC)) {
                // WebP files have RIFF header followed by size then WEBP
                if (header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P') {
                    return true;
                }
            }

            return false;
        } catch (IOException e) {
            log.error("Error reading file magic number", e);
            return false;
        }
    }

    /**
     * Check if byte array starts with given prefix.
     */
    private boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) return false;
        }
        return true;
    }

    /**
     * Extract file extension from filename.
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    /**
     * Validate filename to prevent path traversal attacks.
     * Only allows UUID-style filenames with allowed extensions.
     */
    private boolean isValidFilename(String filename) {
        if (filename == null || filename.isEmpty()) return false;
        // Check for path traversal attempts
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            return false;
        }
        // Validate format: UUID.extension
        return filename.matches("^[a-fA-F0-9\\-]+\\.(jpg|jpeg|png|gif|webp)$");
    }
}
