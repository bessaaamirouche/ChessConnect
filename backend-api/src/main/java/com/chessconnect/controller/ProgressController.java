package com.chessconnect.controller;

import com.chessconnect.dto.progress.ProgressResponse;
import com.chessconnect.model.Progress;
import com.chessconnect.model.enums.ChessLevel;
import com.chessconnect.repository.ProgressRepository;
import com.chessconnect.security.UserDetailsImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/progress")
public class ProgressController {

    private final ProgressRepository progressRepository;

    public ProgressController(ProgressRepository progressRepository) {
        this.progressRepository = progressRepository;
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ProgressResponse> getMyProgress(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        Progress progress = progressRepository.findByStudentId(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("Progress not found for student"));

        return ResponseEntity.ok(ProgressResponse.fromEntity(progress));
    }

    @GetMapping("/levels")
    public ResponseEntity<List<LevelInfo>> getAllLevels() {
        List<LevelInfo> levels = Arrays.stream(ChessLevel.values())
                .map(level -> new LevelInfo(
                        level.name(),
                        level.getDisplayName(),
                        level.getDescription(),
                        level.getOrder(),
                        getLessonsRequired(level)
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(levels);
    }

    @GetMapping("/levels/{level}")
    public ResponseEntity<LevelInfo> getLevelInfo(@PathVariable String level) {
        ChessLevel chessLevel = ChessLevel.valueOf(level.toUpperCase());
        return ResponseEntity.ok(new LevelInfo(
                chessLevel.name(),
                chessLevel.getDisplayName(),
                chessLevel.getDescription(),
                chessLevel.getOrder(),
                getLessonsRequired(chessLevel)
        ));
    }

    private int getLessonsRequired(ChessLevel level) {
        return switch (level) {
            case PION -> 45;
            case CAVALIER -> 45;
            case FOU -> 45;
            case TOUR -> 45;
            case DAME -> 45;
            case ROI -> 0;
        };
    }

    public record LevelInfo(
            String code,
            String displayName,
            String description,
            int order,
            int lessonsRequired
    ) {}
}
