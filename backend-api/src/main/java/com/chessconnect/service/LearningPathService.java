package com.chessconnect.service;

import com.chessconnect.dto.learningpath.CourseResponse;
import com.chessconnect.dto.learningpath.GradeWithCoursesResponse;
import com.chessconnect.dto.learningpath.LearningPathResponse;
import com.chessconnect.dto.learningpath.NextCourseResponse;
import com.chessconnect.dto.student.StudentProfileResponse;
import com.chessconnect.model.Course;
import com.chessconnect.model.Progress;
import com.chessconnect.model.User;
import com.chessconnect.model.UserCourseProgress;
import com.chessconnect.model.enums.ChessLevel;
import com.chessconnect.model.enums.CourseStatus;
import com.chessconnect.repository.CourseRepository;
import com.chessconnect.repository.PendingCourseValidationRepository;
import com.chessconnect.repository.ProgressRepository;
import com.chessconnect.repository.UserCourseProgressRepository;
import com.chessconnect.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class LearningPathService {

    private static final Logger log = LoggerFactory.getLogger(LearningPathService.class);

    private final CourseRepository courseRepository;
    private final UserCourseProgressRepository userCourseProgressRepository;
    private final ProgressRepository progressRepository;
    private final UserRepository userRepository;
    private final PendingCourseValidationRepository pendingValidationRepository;

    public LearningPathService(
        CourseRepository courseRepository,
        UserCourseProgressRepository userCourseProgressRepository,
        ProgressRepository progressRepository,
        UserRepository userRepository,
        PendingCourseValidationRepository pendingValidationRepository
    ) {
        this.courseRepository = courseRepository;
        this.userCourseProgressRepository = userCourseProgressRepository;
        this.progressRepository = progressRepository;
        this.userRepository = userRepository;
        this.pendingValidationRepository = pendingValidationRepository;
    }

    @Transactional(readOnly = true)
    public LearningPathResponse getLearningPath(Long userId) {
        Progress userProgress = progressRepository.findByStudentId(userId)
            .orElse(null);
        ChessLevel currentLevel = userProgress != null ? userProgress.getCurrentLevel() : ChessLevel.A;

        List<Course> allCourses = courseRepository.findAllOrderByGradeAndOrder();
        Map<Long, UserCourseProgress> progressMap = userCourseProgressRepository.findByUserId(userId)
            .stream()
            .collect(Collectors.toMap(p -> p.getCourse().getId(), p -> p));

        // Get teacher names for validated courses
        Set<Long> teacherIds = progressMap.values().stream()
            .map(UserCourseProgress::getValidatedByTeacherId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Map<Long, String> teacherNames = getTeacherNames(teacherIds);

        List<GradeWithCoursesResponse> grades = new ArrayList<>();

        for (ChessLevel grade : ChessLevel.values()) {
            List<Course> gradeCourses = allCourses.stream()
                .filter(c -> c.getGrade() == grade)
                .toList();

            boolean isGradeUnlocked = isGradeAccessible(grade, currentLevel);

            List<CourseResponse> courseResponses = new ArrayList<>();
            int completedCount = 0;

            for (int i = 0; i < gradeCourses.size(); i++) {
                Course course = gradeCourses.get(i);
                UserCourseProgress progress = progressMap.get(course.getId());

                CourseStatus status = determineStatus(progress, isGradeUnlocked, i, gradeCourses, progressMap);

                String teacherName = null;
                if (progress != null && progress.getValidatedByTeacherId() != null) {
                    teacherName = teacherNames.get(progress.getValidatedByTeacherId());
                }

                CourseResponse response = new CourseResponse(
                    course.getId(),
                    course.getTitle(),
                    course.getDescription(),
                    null,
                    course.getGrade(),
                    course.getOrderInGrade(),
                    course.getEstimatedMinutes(),
                    course.getIconName(),
                    status,
                    progress != null ? progress.getStartedAt() : null,
                    progress != null ? progress.getCompletedAt() : null,
                    progress != null ? progress.getValidatedByTeacherId() : null,
                    teacherName,
                    progress != null ? progress.getValidatedAt() : null
                );
                courseResponses.add(response);

                if (status == CourseStatus.COMPLETED) {
                    completedCount++;
                }
            }

            grades.add(GradeWithCoursesResponse.create(grade, courseResponses, completedCount, isGradeUnlocked));
        }

        return LearningPathResponse.create(grades);
    }

    @Transactional(readOnly = true)
    public CourseResponse getCourseDetail(Long userId, Long courseId) {
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new RuntimeException("Course not found"));

        UserCourseProgress progress = userCourseProgressRepository
            .findByUserIdAndCourseId(userId, courseId)
            .orElse(null);

        Progress userProgress = progressRepository.findByStudentId(userId).orElse(null);
        ChessLevel currentLevel = userProgress != null ? userProgress.getCurrentLevel() : ChessLevel.A;
        boolean isGradeUnlocked = isGradeAccessible(course.getGrade(), currentLevel);

        List<Course> gradeCourses = courseRepository.findByGradeOrderByOrderInGrade(course.getGrade());
        Map<Long, UserCourseProgress> progressMap = userCourseProgressRepository.findByUserId(userId)
            .stream()
            .collect(Collectors.toMap(p -> p.getCourse().getId(), p -> p));

        int courseIndex = -1;
        for (int i = 0; i < gradeCourses.size(); i++) {
            if (gradeCourses.get(i).getId().equals(courseId)) {
                courseIndex = i;
                break;
            }
        }

        CourseStatus status = determineStatus(progress, isGradeUnlocked, courseIndex, gradeCourses, progressMap);

        String teacherName = null;
        if (progress != null && progress.getValidatedByTeacherId() != null) {
            teacherName = userRepository.findById(progress.getValidatedByTeacherId())
                .map(u -> u.getFirstName() + " " + u.getLastName())
                .orElse(null);
        }

        return new CourseResponse(
            course.getId(),
            course.getTitle(),
            course.getDescription(),
            status != CourseStatus.LOCKED ? course.getContent() : null,
            course.getGrade(),
            course.getOrderInGrade(),
            course.getEstimatedMinutes(),
            course.getIconName(),
            status,
            progress != null ? progress.getStartedAt() : null,
            progress != null ? progress.getCompletedAt() : null,
            progress != null ? progress.getValidatedByTeacherId() : null,
            teacherName,
            progress != null ? progress.getValidatedAt() : null
        );
    }

    @Transactional
    public CourseResponse startCourse(Long userId, Long courseId) {
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new RuntimeException("Course not found"));

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        Progress userProgress = progressRepository.findByStudentId(userId).orElse(null);
        ChessLevel currentLevel = userProgress != null ? userProgress.getCurrentLevel() : ChessLevel.A;

        if (!isGradeAccessible(course.getGrade(), currentLevel)) {
            throw new RuntimeException("Ce grade n'est pas encore accessible");
        }

        UserCourseProgress progress = userCourseProgressRepository
            .findByUserIdAndCourseId(userId, courseId)
            .orElseGet(() -> {
                UserCourseProgress newProgress = new UserCourseProgress();
                newProgress.setUser(user);
                newProgress.setCourse(course);
                return newProgress;
            });

        if (progress.getStatus() == CourseStatus.LOCKED) {
            progress.start();
            progress = userCourseProgressRepository.save(progress);
        }

        return CourseResponse.fromEntity(course, progress);
    }

    /**
     * Validate a course for a student (Teacher only)
     */
    @Transactional
    public CourseResponse validateCourse(Long teacherId, Long studentId, Long courseId) {
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new RuntimeException("Course not found"));

        User student = userRepository.findById(studentId)
            .orElseThrow(() -> new RuntimeException("Student not found"));

        User teacher = userRepository.findById(teacherId)
            .orElseThrow(() -> new RuntimeException("Teacher not found"));

        UserCourseProgress progress = userCourseProgressRepository
            .findByUserIdAndCourseId(studentId, courseId)
            .orElseGet(() -> {
                UserCourseProgress newProgress = new UserCourseProgress();
                newProgress.setUser(student);
                newProgress.setCourse(course);
                newProgress.start();
                return newProgress;
            });

        if (progress.getStatus() != CourseStatus.COMPLETED) {
            progress.validate(teacherId);
            progress = userCourseProgressRepository.save(progress);

            unlockNextCourse(studentId, course);
        }

        // Resolve pending validations for this teacher/student pair
        try {
            pendingValidationRepository.deleteByTeacherIdAndStudentId(teacherId, studentId);
            log.info("Resolved pending validations for teacher {} and student {}", teacherId, studentId);
        } catch (Exception e) {
            log.warn("Could not resolve pending validations: {}", e.getMessage());
        }

        String teacherName = teacher.getFirstName() + " " + teacher.getLastName();
        return CourseResponse.fromEntity(course, progress, teacherName);
    }

    /**
     * Set the student's level (Teacher only)
     * This unlocks all courses at the given level and below
     */
    @Transactional
    public void setStudentLevel(Long teacherId, Long studentId, ChessLevel level) {
        User student = userRepository.findById(studentId)
            .orElseThrow(() -> new RuntimeException("Student not found"));

        Progress progress = progressRepository.findByStudentId(studentId)
            .orElseGet(() -> {
                Progress newProgress = new Progress();
                newProgress.setStudent(student);
                return newProgress;
            });

        // Update level and mark as evaluated by coach
        progress.setCurrentLevel(level);
        progress.setLevelSetByCoach(true);
        progress.setEvaluatedByTeacherId(teacherId);
        progress.setEvaluatedAt(java.time.LocalDateTime.now());
        progress.setLessonsAtCurrentLevel(0);
        progressRepository.save(progress);

        // Unlock all courses at the given level and below
        unlockCoursesForLevel(student, level);
    }

    /**
     * Unlock all courses at the given level and below
     */
    private void unlockCoursesForLevel(User student, ChessLevel targetLevel) {
        List<Course> allCourses = courseRepository.findAllOrderByGradeAndOrder();

        for (Course course : allCourses) {
            // Only unlock courses at or below the target level
            if (course.getGrade().getOrder() <= targetLevel.getOrder()) {
                // Check if progress already exists
                if (!userCourseProgressRepository.existsByUserIdAndCourseId(student.getId(), course.getId())) {
                    UserCourseProgress newProgress = new UserCourseProgress();
                    newProgress.setUser(student);
                    newProgress.setCourse(course);
                    newProgress.setStatus(CourseStatus.IN_PROGRESS);
                    userCourseProgressRepository.save(newProgress);
                }
            }
        }
    }

    /**
     * Get student profile with all courses (for teachers)
     */
    @Transactional(readOnly = true)
    public StudentProfileResponse getStudentProfile(Long studentId) {
        User student = userRepository.findById(studentId)
            .orElseThrow(() -> new RuntimeException("Student not found"));

        Progress studentProgress = progressRepository.findByStudentId(studentId)
            .orElse(null);
        ChessLevel currentLevel = studentProgress != null ? studentProgress.getCurrentLevel() : ChessLevel.A;
        int totalLessonsCompleted = studentProgress != null ? studentProgress.getTotalLessonsCompleted() : 0;
        Boolean levelSetByCoach = studentProgress != null ? studentProgress.getLevelSetByCoach() : false;
        Long evaluatedByTeacherId = studentProgress != null ? studentProgress.getEvaluatedByTeacherId() : null;
        java.time.LocalDateTime evaluatedAt = studentProgress != null ? studentProgress.getEvaluatedAt() : null;

        // Get evaluator teacher name if exists
        String evaluatedByTeacherName = null;
        if (evaluatedByTeacherId != null) {
            evaluatedByTeacherName = userRepository.findById(evaluatedByTeacherId)
                .map(u -> u.getFirstName() + " " + u.getLastName())
                .orElse(null);
        }

        List<Course> allCourses = courseRepository.findAllOrderByGradeAndOrder();
        Map<Long, UserCourseProgress> progressMap = userCourseProgressRepository.findByUserId(studentId)
            .stream()
            .collect(Collectors.toMap(p -> p.getCourse().getId(), p -> p));

        // Get teacher names for validated courses
        Set<Long> teacherIds = progressMap.values().stream()
            .map(UserCourseProgress::getValidatedByTeacherId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Map<Long, String> teacherNames = getTeacherNames(teacherIds);

        List<GradeWithCoursesResponse> grades = new ArrayList<>();

        for (ChessLevel grade : ChessLevel.values()) {
            List<Course> gradeCourses = allCourses.stream()
                .filter(c -> c.getGrade() == grade)
                .toList();

            boolean isGradeUnlocked = isGradeAccessible(grade, currentLevel);

            List<CourseResponse> courseResponses = new ArrayList<>();
            int completedCount = 0;

            for (int i = 0; i < gradeCourses.size(); i++) {
                Course course = gradeCourses.get(i);
                UserCourseProgress progress = progressMap.get(course.getId());

                CourseStatus status = determineStatus(progress, isGradeUnlocked, i, gradeCourses, progressMap);

                String teacherName = null;
                if (progress != null && progress.getValidatedByTeacherId() != null) {
                    teacherName = teacherNames.get(progress.getValidatedByTeacherId());
                }

                CourseResponse response = new CourseResponse(
                    course.getId(),
                    course.getTitle(),
                    course.getDescription(),
                    null,
                    course.getGrade(),
                    course.getOrderInGrade(),
                    course.getEstimatedMinutes(),
                    course.getIconName(),
                    status,
                    progress != null ? progress.getStartedAt() : null,
                    progress != null ? progress.getCompletedAt() : null,
                    progress != null ? progress.getValidatedByTeacherId() : null,
                    teacherName,
                    progress != null ? progress.getValidatedAt() : null
                );
                courseResponses.add(response);

                if (status == CourseStatus.COMPLETED) {
                    completedCount++;
                }
            }

            grades.add(GradeWithCoursesResponse.create(grade, courseResponses, completedCount, isGradeUnlocked));
        }

        return StudentProfileResponse.create(
            student.getId(),
            student.getFirstName(),
            student.getLastName(),
            currentLevel,
            totalLessonsCompleted,
            levelSetByCoach,
            evaluatedByTeacherId,
            evaluatedByTeacherName,
            evaluatedAt,
            grades
        );
    }

    /**
     * Get the next course for a student (first IN_PROGRESS or first LOCKED course)
     */
    @Transactional(readOnly = true)
    public NextCourseResponse getNextCourseForStudent(Long studentId) {
        Progress studentProgress = progressRepository.findByStudentId(studentId).orElse(null);
        ChessLevel currentLevel = studentProgress != null ? studentProgress.getCurrentLevel() : ChessLevel.A;

        List<Course> allCourses = courseRepository.findAllOrderByGradeAndOrder();
        Map<Long, UserCourseProgress> progressMap = userCourseProgressRepository.findByUserId(studentId)
            .stream()
            .collect(Collectors.toMap(p -> p.getCourse().getId(), p -> p));

        // Find the first course that is IN_PROGRESS or the first accessible LOCKED course
        for (ChessLevel grade : ChessLevel.values()) {
            if (!isGradeAccessible(grade, currentLevel)) {
                continue;
            }

            List<Course> gradeCourses = allCourses.stream()
                .filter(c -> c.getGrade() == grade)
                .toList();

            for (int i = 0; i < gradeCourses.size(); i++) {
                Course course = gradeCourses.get(i);
                UserCourseProgress progress = progressMap.get(course.getId());
                CourseStatus status = determineStatus(progress, true, i, gradeCourses, progressMap);

                if (status == CourseStatus.IN_PROGRESS || status == CourseStatus.PENDING_VALIDATION) {
                    return NextCourseResponse.create(course.getId(), course.getTitle(), grade);
                }
            }
        }

        // If no IN_PROGRESS found, find first LOCKED that would be accessible
        for (ChessLevel grade : ChessLevel.values()) {
            if (!isGradeAccessible(grade, currentLevel)) {
                continue;
            }

            List<Course> gradeCourses = allCourses.stream()
                .filter(c -> c.getGrade() == grade)
                .toList();

            for (int i = 0; i < gradeCourses.size(); i++) {
                Course course = gradeCourses.get(i);
                UserCourseProgress progress = progressMap.get(course.getId());
                CourseStatus status = determineStatus(progress, true, i, gradeCourses, progressMap);

                if (status == CourseStatus.LOCKED) {
                    return NextCourseResponse.create(course.getId(), course.getTitle(), grade);
                }
            }
        }

        // All courses completed
        return null;
    }

    private Map<Long, String> getTeacherNames(Set<Long> teacherIds) {
        if (teacherIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userRepository.findAllById(teacherIds).stream()
            .collect(Collectors.toMap(
                User::getId,
                u -> u.getFirstName() + " " + u.getLastName()
            ));
    }

    private void unlockNextCourse(Long userId, Course completedCourse) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        List<Course> gradeCourses = courseRepository.findByGradeOrderByOrderInGrade(completedCourse.getGrade());

        Optional<Course> nextCourse = gradeCourses.stream()
            .filter(c -> c.getOrderInGrade() > completedCourse.getOrderInGrade())
            .findFirst();

        if (nextCourse.isPresent()) {
            Course next = nextCourse.get();
            if (!userCourseProgressRepository.existsByUserIdAndCourseId(userId, next.getId())) {
                UserCourseProgress newProgress = new UserCourseProgress();
                newProgress.setUser(user);
                newProgress.setCourse(next);
                newProgress.setStatus(CourseStatus.IN_PROGRESS);
                userCourseProgressRepository.save(newProgress);
            }
        }
    }

    private boolean isGradeAccessible(ChessLevel grade, ChessLevel currentUserLevel) {
        return grade.getOrder() <= currentUserLevel.getOrder();
    }

    private CourseStatus determineStatus(
        UserCourseProgress progress,
        boolean isGradeUnlocked,
        int courseIndex,
        List<Course> gradeCourses,
        Map<Long, UserCourseProgress> progressMap
    ) {
        // Keep existing progress status if any (COMPLETED, IN_PROGRESS, PENDING_VALIDATION)
        if (progress != null && progress.getStatus() != CourseStatus.LOCKED) {
            return progress.getStatus();
        }

        // If grade is not unlocked, course is locked
        if (!isGradeUnlocked) {
            return CourseStatus.LOCKED;
        }

        // If grade is unlocked, ALL courses in it are accessible (no sequential requirement)
        return progress != null ? progress.getStatus() : CourseStatus.IN_PROGRESS;
    }
}
