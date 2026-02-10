package com.chessconnect.repository;

import com.chessconnect.model.GroupInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GroupInvitationRepository extends JpaRepository<GroupInvitation, Long> {

    Optional<GroupInvitation> findByToken(String token);

    Optional<GroupInvitation> findByLessonId(Long lessonId);

    void deleteByLessonId(Long lessonId);

    void deleteByCreatedById(Long userId);
}
