package com.chessconnect.service;

import com.chessconnect.dto.rating.CreateRatingRequest;
import com.chessconnect.dto.rating.RatingResponse;
import com.chessconnect.model.Lesson;
import com.chessconnect.model.Rating;
import com.chessconnect.model.User;
import com.chessconnect.model.enums.LessonStatus;
import com.chessconnect.repository.LessonRepository;
import com.chessconnect.repository.RatingRepository;
import com.chessconnect.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RatingService {

    private static final Logger log = LoggerFactory.getLogger(RatingService.class);

    private final RatingRepository ratingRepository;
    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;

    public RatingService(
            RatingRepository ratingRepository,
            LessonRepository lessonRepository,
            UserRepository userRepository
    ) {
        this.ratingRepository = ratingRepository;
        this.lessonRepository = lessonRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    @CacheEvict(value = "ratings", allEntries = true)
    public RatingResponse createRating(Long studentId, CreateRatingRequest request) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));

        Lesson lesson = lessonRepository.findById(request.lessonId())
                .orElseThrow(() -> new IllegalArgumentException("Lesson not found"));

        // Validate: Only the student who took the lesson can rate it
        if (!lesson.getStudent().getId().equals(studentId)) {
            throw new IllegalArgumentException("You can only rate lessons you have taken");
        }

        // Validate: Lesson must be completed
        if (lesson.getStatus() != LessonStatus.COMPLETED) {
            throw new IllegalArgumentException("You can only rate completed lessons");
        }

        // Validate: Lesson not already rated
        if (ratingRepository.existsByLessonId(request.lessonId())) {
            throw new IllegalArgumentException("You have already rated this lesson");
        }

        Rating rating = new Rating();
        rating.setStudent(student);
        rating.setTeacher(lesson.getTeacher());
        rating.setLesson(lesson);
        rating.setStars(request.stars());
        rating.setComment(request.comment());

        Rating savedRating = ratingRepository.save(rating);
        log.info("Rating created: student {} rated teacher {} with {} stars for lesson {}",
                studentId, lesson.getTeacher().getId(), request.stars(), lesson.getId());

        return RatingResponse.from(savedRating);
    }

    @Cacheable(value = "ratings", key = "'teacher-' + #teacherId")
    public List<RatingResponse> getRatingsForTeacher(Long teacherId) {
        return ratingRepository.findByTeacherIdOrderByCreatedAtDesc(teacherId)
                .stream()
                .map(RatingResponse::from)
                .toList();
    }

    public boolean isLessonRated(Long lessonId) {
        return ratingRepository.existsByLessonId(lessonId);
    }

    public RatingResponse getRatingForLesson(Long lessonId) {
        return ratingRepository.findByLessonId(lessonId)
                .map(RatingResponse::from)
                .orElse(null);
    }

    @Cacheable(value = "ratings", key = "'avg-' + #teacherId")
    public Double getAverageRatingForTeacher(Long teacherId) {
        return ratingRepository.getAverageRatingForTeacher(teacherId);
    }

    @Cacheable(value = "ratings", key = "'count-' + #teacherId")
    public Integer getReviewCountForTeacher(Long teacherId) {
        Integer count = ratingRepository.getReviewCountForTeacher(teacherId);
        return count != null ? count : 0;
    }

    public List<Long> getMyRatedLessonIds(Long studentId) {
        return ratingRepository.findByStudentIdOrderByCreatedAtDesc(studentId)
                .stream()
                .map(rating -> rating.getLesson().getId())
                .toList();
    }
}
