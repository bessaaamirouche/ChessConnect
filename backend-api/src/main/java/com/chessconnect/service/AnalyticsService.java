package com.chessconnect.service;

import com.chessconnect.dto.admin.AnalyticsResponse;
import com.chessconnect.dto.admin.AnalyticsResponse.DataPoint;
import com.chessconnect.dto.admin.AnalyticsResponse.HourlyDataPoint;
import com.chessconnect.model.PageView;
import com.chessconnect.model.User;
import com.chessconnect.model.enums.UserRole;
import com.chessconnect.repository.PageViewRepository;
import com.chessconnect.repository.SubscriptionRepository;
import com.chessconnect.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PageViewRepository pageViewRepository;

    public AnalyticsService(
            UserRepository userRepository,
            SubscriptionRepository subscriptionRepository,
            PageViewRepository pageViewRepository
    ) {
        this.userRepository = userRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.pageViewRepository = pageViewRepository;
    }

    /**
     * Get analytics data for the specified period.
     * @param period "day" (last 7 days), "week" (last 4 weeks), "month" (last 12 months)
     */
    @Transactional(readOnly = true)
    public AnalyticsResponse getAnalytics(String period) {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start;

        switch (period) {
            case "week" -> start = end.minusWeeks(4);
            case "month" -> start = end.minusMonths(12);
            default -> start = end.minusDays(7); // "day" - last 7 days
        }

        log.debug("Fetching analytics from {} to {} for period {}", start, end, period);

        // Generate date labels for the period
        List<String> dateLabels = generateDateLabels(start.toLocalDate(), end.toLocalDate());

        // Fetch all data using database aggregations
        List<DataPoint> studentRegistrations = convertToDataPointsWithFill(
                userRepository.countRegistrationsByRoleAndDay(UserRole.STUDENT.name(), start, end),
                dateLabels
        );

        List<DataPoint> teacherRegistrations = convertToDataPointsWithFill(
                userRepository.countRegistrationsByRoleAndDay(UserRole.TEACHER.name(), start, end),
                dateLabels
        );

        List<DataPoint> newSubscriptions = convertToDataPointsWithFill(
                subscriptionRepository.countNewSubscriptionsByDay(start, end),
                dateLabels
        );

        List<DataPoint> renewals = convertToDataPointsWithFill(
                subscriptionRepository.countRenewalsByDay(start, end),
                dateLabels
        );

        List<DataPoint> cancellations = convertToDataPointsWithFill(
                subscriptionRepository.countCancellationsByDay(start, end),
                dateLabels
        );

        List<DataPoint> dailyVisits = convertToDataPointsWithFill(
                pageViewRepository.countVisitsByDay(start, end),
                dateLabels
        );

        List<HourlyDataPoint> hourlyVisits = convertToHourlyDataPoints(
                pageViewRepository.countVisitsByHour(start, end)
        );

        return new AnalyticsResponse(
                studentRegistrations,
                teacherRegistrations,
                newSubscriptions,
                renewals,
                cancellations,
                dailyVisits,
                hourlyVisits
        );
    }

    /**
     * Track a page view.
     */
    @Transactional
    public void trackPageView(Long userId, String pageUrl, String sessionId) {
        PageView pageView = new PageView();

        if (userId != null) {
            pageView.setUser(userRepository.getReferenceById(userId));
        }

        pageView.setPageUrl(pageUrl);
        pageView.setSessionId(sessionId);
        pageView.setVisitedAt(LocalDateTime.now());

        pageViewRepository.save(pageView);
        log.debug("Tracked page view: {} for session {}", pageUrl, sessionId);
    }

    /**
     * Cleanup old page views (runs daily at 3 AM).
     * Retains data for 90 days.
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanOldPageViews() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(90);
        pageViewRepository.deleteByVisitedAtBefore(cutoff);
        log.info("Cleaned up page views older than {}", cutoff);
    }

    /**
     * Generate a list of date labels between start and end (inclusive).
     */
    private List<String> generateDateLabels(LocalDate start, LocalDate end) {
        List<String> labels = new ArrayList<>();
        LocalDate current = start;
        while (!current.isAfter(end)) {
            labels.add(current.format(DATE_FORMATTER));
            current = current.plusDays(1);
        }
        return labels;
    }

    /**
     * Convert query results to DataPoints, filling in zeros for missing dates.
     */
    private List<DataPoint> convertToDataPointsWithFill(List<Object[]> queryResults, List<String> dateLabels) {
        // Convert query results to a map for quick lookup
        Map<String, Long> dataMap = queryResults.stream()
                .collect(Collectors.toMap(
                        row -> {
                            Object dateObj = row[0];
                            if (dateObj instanceof Date sqlDate) {
                                return sqlDate.toLocalDate().format(DATE_FORMATTER);
                            } else if (dateObj instanceof LocalDate localDate) {
                                return localDate.format(DATE_FORMATTER);
                            } else if (dateObj instanceof java.sql.Timestamp timestamp) {
                                return timestamp.toLocalDateTime().toLocalDate().format(DATE_FORMATTER);
                            }
                            return dateObj.toString();
                        },
                        row -> ((Number) row[1]).longValue(),
                        (v1, v2) -> v1 + v2 // Merge duplicates
                ));

        // Fill in all dates with data or zero
        return dateLabels.stream()
                .map(date -> new DataPoint(date, dataMap.getOrDefault(date, 0L)))
                .collect(Collectors.toList());
    }

    /**
     * Convert hourly query results to HourlyDataPoints, filling in zeros for missing hours.
     */
    private List<HourlyDataPoint> convertToHourlyDataPoints(List<Object[]> queryResults) {
        // Convert query results to a map
        Map<Integer, Long> hourMap = queryResults.stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).intValue(),
                        row -> ((Number) row[1]).longValue(),
                        (v1, v2) -> v1 + v2
                ));

        // Fill in all 24 hours
        List<HourlyDataPoint> points = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            points.add(new HourlyDataPoint(hour, hourMap.getOrDefault(hour, 0L)));
        }
        return points;
    }
}
