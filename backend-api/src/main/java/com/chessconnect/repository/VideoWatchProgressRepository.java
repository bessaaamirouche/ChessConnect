package com.chessconnect.repository;

import com.chessconnect.model.VideoWatchProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface VideoWatchProgressRepository extends JpaRepository<VideoWatchProgress, Long> {

    Optional<VideoWatchProgress> findByUserIdAndLessonId(Long userId, Long lessonId);

    @Modifying
    @Transactional
    void deleteByUserIdAndLessonId(Long userId, Long lessonId);
}
