package com.chessconnect.repository;

import com.chessconnect.model.PageView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PageViewRepository extends JpaRepository<PageView, Long> {

    /**
     * Count unique visits (by session) per day within a date range.
     */
    @Query(value = "SELECT DATE(visited_at) as date, COUNT(DISTINCT session_id) as visits " +
           "FROM page_views WHERE visited_at BETWEEN :start AND :end " +
           "GROUP BY DATE(visited_at) ORDER BY date", nativeQuery = true)
    List<Object[]> countVisitsByDay(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Count page views per hour (0-23) within a date range for peak hours analysis.
     */
    @Query(value = "SELECT EXTRACT(HOUR FROM visited_at) as hour, COUNT(*) as count " +
           "FROM page_views WHERE visited_at BETWEEN :start AND :end " +
           "GROUP BY EXTRACT(HOUR FROM visited_at) ORDER BY hour", nativeQuery = true)
    List<Object[]> countVisitsByHour(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Count total unique visits (by session) within a date range.
     */
    @Query("SELECT COUNT(DISTINCT p.sessionId) FROM PageView p WHERE p.visitedAt BETWEEN :start AND :end")
    Long countUniqueVisits(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Delete old page views for data retention (cleanup job).
     */
    @Modifying
    @Query("DELETE FROM PageView p WHERE p.visitedAt < :cutoff")
    void deleteByVisitedAtBefore(@Param("cutoff") LocalDateTime cutoff);
}
