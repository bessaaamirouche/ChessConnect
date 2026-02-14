package com.chessconnect.controller;

import com.chessconnect.dto.VideoDTO;
import com.chessconnect.model.Lesson;
import com.chessconnect.model.User;
import com.chessconnect.repository.LessonParticipantRepository;
import com.chessconnect.repository.LessonRepository;
import com.chessconnect.repository.UserRepository;
import com.chessconnect.service.SubscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/library")
public class LibraryController {

    private static final Logger log = LoggerFactory.getLogger(LibraryController.class);
    private static final String DEFAULT_THUMBNAIL = "/assets/images/video-thumbnail-placeholder.svg";

    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;
    private final LessonParticipantRepository participantRepository;
    private final SubscriptionService subscriptionService;

    public LibraryController(LessonRepository lessonRepository, UserRepository userRepository,
                             LessonParticipantRepository participantRepository, SubscriptionService subscriptionService) {
        this.lessonRepository = lessonRepository;
        this.userRepository = userRepository;
        this.participantRepository = participantRepository;
        this.subscriptionService = subscriptionService;
    }

    /**
     * Get all videos (completed lessons with recordings) for the current student.
     * Requires Premium subscription or active free trial.
     * Supports search by teacher name and date filtering.
     */
    @GetMapping("/videos")
    public ResponseEntity<List<VideoDTO>> getMyVideos(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) String period
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String userEmail = authentication.getName();
        User user = userRepository.findByEmail(userEmail).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Only students can access the library
        if (!"STUDENT".equals(user.getRole().name())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // All students can access their library (videos recorded during their premium period are kept)

        // Calculate date range based on period or explicit dates
        LocalDateTime dateFromTime = null;
        LocalDateTime dateToTime = null;

        if (period != null && !period.isEmpty()) {
            LocalDate now = LocalDate.now();
            switch (period) {
                case "week":
                    dateFromTime = now.minusWeeks(1).atStartOfDay();
                    break;
                case "month":
                    dateFromTime = now.minusMonths(1).atStartOfDay();
                    break;
                case "3months":
                    dateFromTime = now.minusMonths(3).atStartOfDay();
                    break;
                case "year":
                    dateFromTime = now.minusYears(1).atStartOfDay();
                    break;
            }
            dateToTime = LocalDateTime.now();
        } else {
            if (dateFrom != null) {
                dateFromTime = dateFrom.atStartOfDay();
            }
            if (dateTo != null) {
                dateToTime = dateTo.atTime(LocalTime.MAX);
            }
        }

        // Get filtered videos
        List<Lesson> lessonsWithRecordings = lessonRepository.findLibraryVideos(
            user.getId(),
            search,
            dateFromTime,
            dateToTime
        );

        // Filter out group lessons where the student is not premium
        // (only premium participants can see group lesson recordings)
        boolean isPremium = subscriptionService.isPremium(user.getId());
        List<VideoDTO> videos = lessonsWithRecordings.stream()
            .filter(lesson -> {
                if (Boolean.TRUE.equals(lesson.getIsGroupLesson())) {
                    // For group lessons, only premium students can see recordings
                    // If the student is the direct student (created the group), still need premium
                    return isPremium;
                }
                return true; // Private lessons are always visible
            })
            .map(this::mapToVideoDTO)
            .collect(Collectors.toList());

        log.info("User {} accessed library with {} videos (search={}, period={})",
                user.getId(), videos.size(), search, period);
        return ResponseEntity.ok(videos);
    }

    /**
     * Delete a video from the library (soft delete - marks as deleted by student)
     */
    @DeleteMapping("/videos/{lessonId}")
    public ResponseEntity<Void> deleteVideo(@PathVariable Long lessonId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String userEmail = authentication.getName();
        User user = userRepository.findByEmail(userEmail).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Lesson lesson = lessonRepository.findById(lessonId).orElse(null);
        if (lesson == null) {
            return ResponseEntity.notFound().build();
        }

        // Verify the lesson belongs to this student (direct or group participant)
        boolean isDirectStudent = lesson.getStudent() != null && lesson.getStudent().getId().equals(user.getId());
        boolean isGroupParticipant = Boolean.TRUE.equals(lesson.getIsGroupLesson())
                && participantRepository.existsActiveByLessonIdAndStudentId(lessonId, user.getId());
        if (!isDirectStudent && !isGroupParticipant) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Soft delete
        lesson.setDeletedByStudent(true);
        lessonRepository.save(lesson);

        log.info("User {} deleted video for lesson {}", user.getId(), lessonId);
        return ResponseEntity.noContent().build();
    }

    private VideoDTO mapToVideoDTO(Lesson lesson) {
        User teacher = lesson.getTeacher();

        VideoDTO dto = new VideoDTO();
        dto.setId(lesson.getId());
        dto.setLessonId(lesson.getId());
        dto.setTeacherName(teacher.getFirstName() + " " + teacher.getLastName().charAt(0) + ".");
        dto.setTeacherAvatar(teacher.getAvatarUrl());
        dto.setScheduledAt(lesson.getScheduledAt());
        dto.setRecordingUrl(lesson.getRecordingUrl());
        dto.setDurationSeconds(lesson.getDurationMinutes() != null ? lesson.getDurationMinutes() * 60 : 3600);
        // Use generated thumbnail if available, otherwise use default placeholder
        dto.setThumbnailUrl(lesson.getThumbnailUrl() != null ? lesson.getThumbnailUrl() : DEFAULT_THUMBNAIL);
        // Set course title if available
        if (lesson.getCourse() != null) {
            dto.setCourseTitle(lesson.getCourse().getTitle());
        }

        return dto;
    }
}
