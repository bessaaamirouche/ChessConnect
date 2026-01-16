package com.chessconnect.service;

import com.chessconnect.dto.favorite.FavoriteTeacherResponse;
import com.chessconnect.model.FavoriteTeacher;
import com.chessconnect.model.User;
import com.chessconnect.model.enums.UserRole;
import com.chessconnect.repository.FavoriteTeacherRepository;
import com.chessconnect.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FavoriteTeacherService {

    private static final Logger log = LoggerFactory.getLogger(FavoriteTeacherService.class);

    private final FavoriteTeacherRepository favoriteRepository;
    private final UserRepository userRepository;

    public FavoriteTeacherService(
            FavoriteTeacherRepository favoriteRepository,
            UserRepository userRepository
    ) {
        this.favoriteRepository = favoriteRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public FavoriteTeacherResponse addFavorite(Long studentId, Long teacherId) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));

        if (student.getRole() != UserRole.STUDENT) {
            throw new IllegalArgumentException("Only students can add favorites");
        }

        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found"));

        if (teacher.getRole() != UserRole.TEACHER) {
            throw new IllegalArgumentException("Can only favorite teachers");
        }

        if (favoriteRepository.existsByStudentIdAndTeacherId(studentId, teacherId)) {
            throw new IllegalArgumentException("Teacher is already in favorites");
        }

        FavoriteTeacher favorite = new FavoriteTeacher();
        favorite.setStudent(student);
        favorite.setTeacher(teacher);
        favorite.setNotifyNewSlots(false);

        FavoriteTeacher saved = favoriteRepository.save(favorite);
        log.info("Student {} added teacher {} to favorites", studentId, teacherId);

        return FavoriteTeacherResponse.from(saved);
    }

    @Transactional
    public void removeFavorite(Long studentId, Long teacherId) {
        favoriteRepository.deleteByStudentIdAndTeacherId(studentId, teacherId);
        log.info("Student {} removed teacher {} from favorites", studentId, teacherId);
    }

    public List<FavoriteTeacherResponse> getFavorites(Long studentId) {
        return favoriteRepository.findByStudentIdOrderByCreatedAtDesc(studentId)
                .stream()
                .map(FavoriteTeacherResponse::from)
                .toList();
    }

    public boolean isFavorite(Long studentId, Long teacherId) {
        return favoriteRepository.existsByStudentIdAndTeacherId(studentId, teacherId);
    }

    @Transactional
    public FavoriteTeacherResponse updateNotifications(Long studentId, Long teacherId, boolean notify) {
        FavoriteTeacher favorite = favoriteRepository.findByStudentIdAndTeacherId(studentId, teacherId)
                .orElseThrow(() -> new IllegalArgumentException("Favorite not found"));

        favorite.setNotifyNewSlots(notify);
        FavoriteTeacher saved = favoriteRepository.save(favorite);
        log.info("Student {} {} notifications for teacher {}",
                studentId, notify ? "enabled" : "disabled", teacherId);

        return FavoriteTeacherResponse.from(saved);
    }

    public List<FavoriteTeacher> getSubscribersForTeacher(Long teacherId) {
        return favoriteRepository.findByTeacherIdAndNotifyNewSlotsTrue(teacherId);
    }
}
