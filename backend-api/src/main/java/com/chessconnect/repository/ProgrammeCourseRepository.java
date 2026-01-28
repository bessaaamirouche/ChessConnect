package com.chessconnect.repository;

import com.chessconnect.model.ProgrammeCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProgrammeCourseRepository extends JpaRepository<ProgrammeCourse, Integer> {

    List<ProgrammeCourse> findByLevelCodeOrderByCourseOrder(String levelCode);

    List<ProgrammeCourse> findAllByOrderByIdAsc();

    @Query("SELECT MAX(pc.id) FROM ProgrammeCourse pc")
    Integer findMaxCourseId();

    Optional<ProgrammeCourse> findFirstByIdGreaterThanOrderByIdAsc(Integer currentId);

    Optional<ProgrammeCourse> findFirstByIdLessThanOrderByIdDesc(Integer currentId);
}
