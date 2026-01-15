package com.chessconnect.repository;

import com.chessconnect.model.Course;
import com.chessconnect.model.enums.ChessLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findByGradeOrderByOrderInGrade(ChessLevel grade);

    @Query("SELECT c FROM Course c ORDER BY c.grade, c.orderInGrade")
    List<Course> findAllOrderByGradeAndOrder();

    Optional<Course> findByGradeAndOrderInGrade(ChessLevel grade, Integer orderInGrade);

    long countByGrade(ChessLevel grade);

    boolean existsByGrade(ChessLevel grade);
}
