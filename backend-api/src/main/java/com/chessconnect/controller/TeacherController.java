package com.chessconnect.controller;

import com.chessconnect.dto.teacher.TeacherBalanceResponse;
import com.chessconnect.model.User;
import com.chessconnect.model.enums.UserRole;
import com.chessconnect.repository.LessonRepository;
import com.chessconnect.repository.UserRepository;
import com.chessconnect.service.RatingService;
import com.chessconnect.service.TeacherBalanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/teachers")
public class TeacherController {

    private final UserRepository userRepository;
    private final TeacherBalanceService teacherBalanceService;
    private final RatingService ratingService;
    private final LessonRepository lessonRepository;

    public TeacherController(
            UserRepository userRepository,
            TeacherBalanceService teacherBalanceService,
            RatingService ratingService,
            LessonRepository lessonRepository
    ) {
        this.userRepository = userRepository;
        this.teacherBalanceService = teacherBalanceService;
        this.ratingService = ratingService;
        this.lessonRepository = lessonRepository;
    }

    @GetMapping
    public ResponseEntity<List<TeacherResponse>> getAllTeachers() {
        List<User> teachers = userRepository.findByRole(UserRole.TEACHER);
        List<TeacherResponse> response = teachers.stream()
                .map(this::mapToResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TeacherResponse> getTeacher(@PathVariable Long id) {
        User teacher = userRepository.findById(id)
                .filter(u -> u.getRole() == UserRole.TEACHER)
                .orElseThrow(() -> new RuntimeException("Teacher not found"));
        return ResponseEntity.ok(mapToResponse(teacher));
    }

    @GetMapping("/search")
    public ResponseEntity<List<TeacherResponse>> searchTeachers(@RequestParam String q) {
        List<User> teachers = userRepository.findByRole(UserRole.TEACHER).stream()
                .filter(t -> t.getFirstName().toLowerCase().contains(q.toLowerCase()) ||
                             t.getLastName().toLowerCase().contains(q.toLowerCase()) ||
                             (t.getBio() != null && t.getBio().toLowerCase().contains(q.toLowerCase())))
                .toList();
        List<TeacherResponse> response = teachers.stream()
                .map(this::mapToResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/subscription")
    public ResponseEntity<List<TeacherResponse>> getTeachersAcceptingSubscription() {
        List<User> teachers = userRepository.findByRole(UserRole.TEACHER).stream()
                .filter(t -> Boolean.TRUE.equals(t.getAcceptsSubscription()))
                .toList();
        List<TeacherResponse> response = teachers.stream()
                .map(this::mapToResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me/balance")
    public ResponseEntity<TeacherBalanceResponse> getMyBalance(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() != UserRole.TEACHER) {
            throw new RuntimeException("Only teachers can access balance");
        }

        return ResponseEntity.ok(teacherBalanceService.getBalance(user.getId()));
    }

    /**
     * Migration endpoint: credit earnings for all completed lessons that haven't been credited yet.
     * This is a one-time operation to fix historical data.
     */
    @PostMapping("/admin/migrate-earnings")
    public ResponseEntity<MigrationResponse> migrateEarnings() {
        int count = teacherBalanceService.migrateUncreditedLessons();
        return ResponseEntity.ok(new MigrationResponse(count, "Migration completed successfully"));
    }

    public record MigrationResponse(int lessonsProcessed, String message) {}

    private TeacherResponse mapToResponse(User teacher) {
        // Parse languages from comma-separated string to List
        List<String> languagesList = teacher.getLanguages() != null && !teacher.getLanguages().isEmpty()
                ? List.of(teacher.getLanguages().split(","))
                : List.of("FR");

        // Get rating data
        Double averageRating = ratingService.getAverageRatingForTeacher(teacher.getId());
        Integer reviewCount = ratingService.getReviewCountForTeacher(teacher.getId());

        // Get lesson stats
        Integer lessonsCompleted = lessonRepository.countCompletedLessonsByTeacherId(teacher.getId());
        Integer totalStudents = lessonRepository.countDistinctStudentsByTeacherId(teacher.getId());

        return new TeacherResponse(
                teacher.getId(),
                teacher.getFirstName(),
                teacher.getLastName(),
                teacher.getHourlyRateCents(),
                teacher.getAcceptsSubscription(),
                teacher.getBio(),
                teacher.getAvatarUrl(),
                languagesList,
                averageRating,
                reviewCount,
                lessonsCompleted != null ? lessonsCompleted : 0,
                totalStudents != null ? totalStudents : 0
        );
    }

    public record TeacherResponse(
            Long id,
            String firstName,
            String lastName,
            Integer hourlyRateCents,
            Boolean acceptsSubscription,
            String bio,
            String avatarUrl,
            List<String> languages,
            Double averageRating,
            Integer reviewCount,
            Integer lessonsCompleted,
            Integer totalStudents
    ) {}
}
