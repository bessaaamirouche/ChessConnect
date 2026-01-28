package com.chessconnect.service;

import com.chessconnect.dto.programme.ProgrammeCourseResponse;
import com.chessconnect.model.ProgrammeCourse;
import com.chessconnect.model.User;
import com.chessconnect.repository.ProgrammeCourseRepository;
import com.chessconnect.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProgrammeService {

    private final ProgrammeCourseRepository programmeCourseRepository;
    private final UserRepository userRepository;

    public ProgrammeService(ProgrammeCourseRepository programmeCourseRepository,
                           UserRepository userRepository) {
        this.programmeCourseRepository = programmeCourseRepository;
        this.userRepository = userRepository;
    }

    public List<ProgrammeCourseResponse> getAllCourses(Long userId) {
        Integer currentCourseId = 1;

        if (userId != null) {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null && user.getCurrentCourseId() != null) {
                currentCourseId = user.getCurrentCourseId();
            }
        }

        final Integer finalCurrentCourseId = currentCourseId;

        return programmeCourseRepository.findAllByOrderByIdAsc().stream()
            .map(course -> new ProgrammeCourseResponse(
                course.getId(),
                course.getLevelCode(),
                course.getLevelName(),
                course.getCourseOrder(),
                course.getTitle(),
                course.getId().equals(finalCurrentCourseId),
                course.getId() < finalCurrentCourseId
            ))
            .collect(Collectors.toList());
    }

    public Map<String, List<ProgrammeCourseResponse>> getCoursesByLevel(Long userId) {
        return getAllCourses(userId).stream()
            .collect(Collectors.groupingBy(ProgrammeCourseResponse::levelCode));
    }

    public ProgrammeCourseResponse getCurrentCourse(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        Integer currentCourseId = user.getCurrentCourseId();
        if (currentCourseId == null) currentCourseId = 1;

        final Integer finalCurrentCourseId = currentCourseId;

        ProgrammeCourse course = programmeCourseRepository.findById(currentCourseId)
            .orElseThrow(() -> new RuntimeException("Course not found"));

        return new ProgrammeCourseResponse(
            course.getId(),
            course.getLevelCode(),
            course.getLevelName(),
            course.getCourseOrder(),
            course.getTitle(),
            true,
            false
        );
    }

    @Transactional
    public ProgrammeCourseResponse setCurrentCourse(Long userId, Integer courseId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        ProgrammeCourse course = programmeCourseRepository.findById(courseId)
            .orElseThrow(() -> new RuntimeException("Course not found"));

        user.setCurrentCourseId(courseId);
        userRepository.save(user);

        return new ProgrammeCourseResponse(
            course.getId(),
            course.getLevelCode(),
            course.getLevelName(),
            course.getCourseOrder(),
            course.getTitle(),
            true,
            false
        );
    }

    @Transactional
    public ProgrammeCourseResponse advanceToNextCourse(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        Integer currentId = user.getCurrentCourseId();
        if (currentId == null) currentId = 1;
        final Integer finalCurrentId = currentId;

        // Find next course
        return programmeCourseRepository.findFirstByIdGreaterThanOrderByIdAsc(finalCurrentId)
            .map(nextCourse -> {
                user.setCurrentCourseId(nextCourse.getId());
                userRepository.save(user);
                return new ProgrammeCourseResponse(
                    nextCourse.getId(),
                    nextCourse.getLevelCode(),
                    nextCourse.getLevelName(),
                    nextCourse.getCourseOrder(),
                    nextCourse.getTitle(),
                    true,
                    false
                );
            })
            .orElseGet(() -> {
                // Already at the last course
                ProgrammeCourse currentCourse = programmeCourseRepository.findById(finalCurrentId)
                    .orElseThrow(() -> new RuntimeException("Course not found"));
                return new ProgrammeCourseResponse(
                    currentCourse.getId(),
                    currentCourse.getLevelCode(),
                    currentCourse.getLevelName(),
                    currentCourse.getCourseOrder(),
                    currentCourse.getTitle(),
                    true,
                    false
                );
            });
    }

    @Transactional
    public ProgrammeCourseResponse goBackToPreviousCourse(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        Integer currentCourseId = user.getCurrentCourseId();
        if (currentCourseId == null || currentCourseId <= 1) {
            // Already at first course
            ProgrammeCourse firstCourse = programmeCourseRepository.findById(1)
                .orElseThrow(() -> new RuntimeException("Course not found"));
            return new ProgrammeCourseResponse(
                firstCourse.getId(),
                firstCourse.getLevelCode(),
                firstCourse.getLevelName(),
                firstCourse.getCourseOrder(),
                firstCourse.getTitle(),
                true,
                false
            );
        }

        // Find previous course
        return programmeCourseRepository.findFirstByIdLessThanOrderByIdDesc(currentCourseId)
            .map(prevCourse -> {
                user.setCurrentCourseId(prevCourse.getId());
                userRepository.save(user);
                return new ProgrammeCourseResponse(
                    prevCourse.getId(),
                    prevCourse.getLevelCode(),
                    prevCourse.getLevelName(),
                    prevCourse.getCourseOrder(),
                    prevCourse.getTitle(),
                    true,
                    false
                );
            })
            .orElseGet(() -> getCurrentCourse(userId));
    }

    public Integer getMaxCourseId() {
        return programmeCourseRepository.findMaxCourseId();
    }
}
