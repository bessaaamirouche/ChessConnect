package com.chessconnect.repository;

import com.chessconnect.model.Availability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface AvailabilityRepository extends JpaRepository<Availability, Long> {

    List<Availability> findByTeacherIdAndIsActiveTrue(Long teacherId);

    List<Availability> findByTeacherIdAndIsRecurringTrueAndIsActiveTrue(Long teacherId);

    List<Availability> findByTeacherIdAndDayOfWeekAndIsActiveTrue(Long teacherId, DayOfWeek dayOfWeek);

    @Query("SELECT a FROM Availability a WHERE a.teacher.id = :teacherId " +
           "AND a.isActive = true AND a.isRecurring = false " +
           "AND a.specificDate = :date")
    List<Availability> findSpecificDateAvailabilities(
            @Param("teacherId") Long teacherId,
            @Param("date") LocalDate date
    );

    @Query("SELECT a FROM Availability a WHERE a.teacher.id = :teacherId " +
           "AND a.isActive = true " +
           "AND ((a.isRecurring = true AND a.dayOfWeek = :dayOfWeek) " +
           "OR (a.isRecurring = false AND a.specificDate = :date))")
    List<Availability> findAvailabilitiesForDate(
            @Param("teacherId") Long teacherId,
            @Param("dayOfWeek") DayOfWeek dayOfWeek,
            @Param("date") LocalDate date
    );

    void deleteByTeacherIdAndId(Long teacherId, Long id);

    void deleteByTeacherId(Long teacherId);
}
