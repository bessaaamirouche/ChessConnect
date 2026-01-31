package com.chessconnect.service;

import com.chessconnect.dto.PendingValidationResponse;
import com.chessconnect.model.Lesson;
import com.chessconnect.model.PendingCourseValidation;
import com.chessconnect.model.User;
import com.chessconnect.repository.LessonRepository;
import com.chessconnect.repository.PendingCourseValidationRepository;
import com.chessconnect.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PendingValidationService {

    private static final Logger log = LoggerFactory.getLogger(PendingValidationService.class);

    private final PendingCourseValidationRepository pendingValidationRepository;
    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;

    public PendingValidationService(
            PendingCourseValidationRepository pendingValidationRepository,
            LessonRepository lessonRepository,
            UserRepository userRepository) {
        this.pendingValidationRepository = pendingValidationRepository;
        this.lessonRepository = lessonRepository;
        this.userRepository = userRepository;
    }

    /**
     * Create a pending validation entry when a lesson is completed
     */
    @Transactional
    public void createPendingValidation(Long lessonId, Long teacherId, Long studentId) {
        // Check if already exists
        if (pendingValidationRepository.existsByLessonId(lessonId)) {
            log.debug("Pending validation already exists for lesson {}", lessonId);
            return;
        }

        Lesson lesson = lessonRepository.findById(lessonId)
            .orElseThrow(() -> new RuntimeException("Lesson not found"));
        User teacher = userRepository.findById(teacherId)
            .orElseThrow(() -> new RuntimeException("Teacher not found"));
        User student = userRepository.findById(studentId)
            .orElseThrow(() -> new RuntimeException("Student not found"));

        PendingCourseValidation pending = new PendingCourseValidation(lesson, teacher, student);
        pendingValidationRepository.save(pending);

        log.info("Created pending validation for lesson {} (teacher: {}, student: {})",
            lessonId, teacherId, studentId);
    }

    /**
     * Get all pending validations for a teacher
     */
    @Transactional(readOnly = true)
    public List<PendingValidationResponse> getPendingValidations(Long teacherId) {
        return pendingValidationRepository.findByTeacherIdAndDismissedFalseOrderByCreatedAtDesc(teacherId)
            .stream()
            .map(PendingValidationResponse::fromEntity)
            .toList();
    }

    /**
     * Dismiss a pending validation
     */
    @Transactional
    public void dismissPendingValidation(Long id, Long teacherId) {
        PendingCourseValidation pending = pendingValidationRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Pending validation not found"));

        if (!pending.getTeacher().getId().equals(teacherId)) {
            throw new RuntimeException("Unauthorized: not your pending validation");
        }

        pending.setDismissed(true);
        pendingValidationRepository.save(pending);
        log.info("Dismissed pending validation {}", id);
    }

    /**
     * Resolve pending validations when a course is validated
     * Removes all pending validations for this teacher/student pair
     */
    @Transactional
    public void resolvePendingValidations(Long teacherId, Long studentId) {
        pendingValidationRepository.deleteByTeacherIdAndStudentId(teacherId, studentId);
        log.info("Resolved pending validations for teacher {} and student {}", teacherId, studentId);
    }

    /**
     * Delete pending validation for a specific lesson
     */
    @Transactional
    public void deletePendingValidationByLesson(Long lessonId) {
        pendingValidationRepository.deleteByLessonId(lessonId);
        log.debug("Deleted pending validation for lesson {}", lessonId);
    }

    /**
     * Get count of pending validations for a teacher
     */
    @Transactional(readOnly = true)
    public int getPendingValidationCount(Long teacherId) {
        return pendingValidationRepository.countByTeacherIdAndDismissedFalse(teacherId);
    }
}
