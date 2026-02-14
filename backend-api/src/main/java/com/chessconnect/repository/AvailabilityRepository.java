package com.chessconnect.repository;

import com.chessconnect.model.Availability;
import com.chessconnect.model.enums.LessonType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

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

    @Query("SELECT DISTINCT a.teacher.id FROM Availability a WHERE a.isActive = true AND a.lessonType = :lessonType")
    List<Long> findDistinctTeacherIdsByLessonTypeAndIsActiveTrue(@Param("lessonType") LessonType lessonType);

    /**
     * Find a GROUP availability for a teacher at a given time.
     * Matches either recurring (by dayOfWeek + time range) or specific date.
     */
    @Query("SELECT a FROM Availability a WHERE a.teacher.id = :teacherId " +
           "AND a.lessonType = com.chessconnect.model.enums.LessonType.GROUP " +
           "AND a.isActive = true " +
           "AND ((a.isRecurring = true AND a.dayOfWeek = :dayOfWeek AND a.startTime <= :slotTime AND a.endTime > :slotTime) " +
           "OR (a.isRecurring = false AND a.specificDate = :date AND a.startTime <= :slotTime AND a.endTime > :slotTime))")
    Optional<Availability> findGroupAvailabilityForSlot(
            @Param("teacherId") Long teacherId,
            @Param("dayOfWeek") DayOfWeek dayOfWeek,
            @Param("date") LocalDate date,
            @Param("slotTime") LocalTime slotTime
    );

    @Modifying
    void deleteByTeacherIdAndId(Long teacherId, Long id);

    @Modifying
    void deleteByTeacherId(Long teacherId);
}
