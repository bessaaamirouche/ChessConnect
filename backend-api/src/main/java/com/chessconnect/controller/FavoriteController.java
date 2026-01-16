package com.chessconnect.controller;

import com.chessconnect.dto.favorite.FavoriteTeacherResponse;
import com.chessconnect.dto.favorite.UpdateNotifyRequest;
import com.chessconnect.model.User;
import com.chessconnect.repository.UserRepository;
import com.chessconnect.service.FavoriteTeacherService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/favorites")
public class FavoriteController {

    private final FavoriteTeacherService favoriteService;
    private final UserRepository userRepository;

    public FavoriteController(FavoriteTeacherService favoriteService, UserRepository userRepository) {
        this.favoriteService = favoriteService;
        this.userRepository = userRepository;
    }

    @PostMapping("/{teacherId}")
    public ResponseEntity<FavoriteTeacherResponse> addFavorite(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long teacherId
    ) {
        User user = getUser(userDetails);
        FavoriteTeacherResponse favorite = favoriteService.addFavorite(user.getId(), teacherId);
        return ResponseEntity.ok(favorite);
    }

    @DeleteMapping("/{teacherId}")
    public ResponseEntity<Void> removeFavorite(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long teacherId
    ) {
        User user = getUser(userDetails);
        favoriteService.removeFavorite(user.getId(), teacherId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<FavoriteTeacherResponse>> getFavorites(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = getUser(userDetails);
        List<FavoriteTeacherResponse> favorites = favoriteService.getFavorites(user.getId());
        return ResponseEntity.ok(favorites);
    }

    @GetMapping("/{teacherId}/status")
    public ResponseEntity<Map<String, Boolean>> getFavoriteStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long teacherId
    ) {
        User user = getUser(userDetails);
        boolean isFavorite = favoriteService.isFavorite(user.getId(), teacherId);
        return ResponseEntity.ok(Map.of("isFavorite", isFavorite));
    }

    @PatchMapping("/{teacherId}/notify")
    public ResponseEntity<FavoriteTeacherResponse> updateNotifications(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long teacherId,
            @Valid @RequestBody UpdateNotifyRequest request
    ) {
        User user = getUser(userDetails);
        FavoriteTeacherResponse favorite = favoriteService.updateNotifications(
                user.getId(), teacherId, request.notifyNewSlots()
        );
        return ResponseEntity.ok(favorite);
    }

    private User getUser(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
