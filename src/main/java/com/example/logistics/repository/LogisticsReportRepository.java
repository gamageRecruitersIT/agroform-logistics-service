package com.example.logistics.repository;

import com.example.logistics.dto.response.DistrictPerformanceResponse;
import com.example.logistics.dto.response.LogisticsSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
//@ConditionalOnBean(JdbcTemplate.class)
public class LogisticsReportRepository {

    private final JdbcTemplate jdbcTemplate;

    public LogisticsSummaryResponse findSummary() {
        return jdbcTemplate.queryForObject("""
                SELECT
                    COUNT(*) AS total_requests,
                    COUNT(*) FILTER (WHERE request_status = 'PENDING') AS pending_requests,
                    COUNT(*) FILTER (WHERE request_status = 'ACCEPTED') AS accepted_requests,
                    COUNT(*) FILTER (WHERE request_status = 'COMPLETED') AS completed_requests,
                    COUNT(*) FILTER (WHERE request_status = 'CANCELLED') AS cancelled_requests,
                    (SELECT COUNT(*) FROM logistics_event) AS total_events
                FROM transport_request
                """, (resultSet, rowNum) -> new LogisticsSummaryResponse(
                resultSet.getLong("total_requests"),
                resultSet.getLong("pending_requests"),
                resultSet.getLong("accepted_requests"),
                resultSet.getLong("completed_requests"),
                resultSet.getLong("cancelled_requests"),
                resultSet.getLong("total_events")
        ));
    }

    public List<DistrictPerformanceResponse> findDistrictPerformance() {
        return jdbcTemplate.query("""
                SELECT
                    COALESCE(NULLIF(TRIM(pickup_district), ''), 'UNKNOWN') AS district,
                    COUNT(*) AS total_requests,
                    COUNT(*) FILTER (WHERE request_status = 'COMPLETED') AS completed_requests,
                    COALESCE(AVG(estimated_distance_km), 0) AS average_distance_km
                FROM transport_request
                GROUP BY COALESCE(NULLIF(TRIM(pickup_district), ''), 'UNKNOWN')
                ORDER BY total_requests DESC, district ASC
                """, (resultSet, rowNum) -> new DistrictPerformanceResponse(
                resultSet.getString("district"),
                resultSet.getLong("total_requests"),
                resultSet.getLong("completed_requests"),
                resultSet.getBigDecimal("average_distance_km")
        ));
    }
}