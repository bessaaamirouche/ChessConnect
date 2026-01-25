package com.chessconnect.dto.admin;

import java.util.List;

public record AnalyticsResponse(
    List<DataPoint> studentRegistrations,
    List<DataPoint> teacherRegistrations,
    List<DataPoint> newSubscriptions,
    List<DataPoint> renewals,
    List<DataPoint> cancellations,
    List<DataPoint> dailyVisits,
    List<HourlyDataPoint> hourlyVisits
) {
    public record DataPoint(String date, Long value) {}
    public record HourlyDataPoint(Integer hour, Long value) {}
}
