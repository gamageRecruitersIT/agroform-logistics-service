package com.example.logistics.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Raw counts/sums returned to Monitoring & Reporting. Rates are NOT calculated here:
 * Monitoring derives them from these numerators and denominators.
 *
 * Reporting window: transport_request.created_at in [from, to) (half-open, UTC instants).
 * Distance is in KM, money in LKR (as stored by Logistics).
 */
public final class TransportReportData {

    private TransportReportData() {}

    public record Window(OffsetDateTime from, OffsetDateTime to, String district, OffsetDateTime generatedAt) {}

    public record TransportSummary(
            Window window,
            long totalRequests,
            long pendingRequests,
            long acceptedRequests,
            long rejectedRequests,
            long cancelledRequests,
            long completedRequests,
            long requestsWithoutDistrict,
            long totalTasks,
            long assignedTasks,
            long inProgressTasks,
            long completedTasks,
            long cancelledTasks,
            BigDecimal totalDistanceKm,
            long distanceSampleCount,
            BigDecimal totalEstimatedCost,
            long estimatedCostSampleCount,
            BigDecimal totalActualCost,
            long actualCostSampleCount) {}

    public record DeliveryPerformance(
            Window window,
            long tasksInWindow,
            long tasksUnloaded,
            BigDecimal avgAssignmentToUnloadingMinutes,
            BigDecimal minAssignmentToUnloadingMinutes,
            BigDecimal maxAssignmentToUnloadingMinutes,
            // No source-provided timing baseline (expected delivery time) exists in Logistics yet.
            boolean timingBaselineAvailable,
            long onTimeEligibleTasks,
            long onTimeTasks,
            String timingNote) {}

    public record DelayReasonCount(String reason, long delayEpisodes) {}

    public record DelaySummary(
            Window window,
            long tasksInWindow,
            long delayedTasks,          // DISTINCT tasks that were ever flagged delayed
            long delayEpisodes,         // DELAY_FLAGGED rows (a task can be flagged more than once)
            long resolvedEpisodes,
            long currentlyDelayedTasks,
            BigDecimal avgResolvedDelayMinutes,
            List<DelayReasonCount> topReasons) {}

    public record DistrictBreakdown(
            String district,
            boolean districtMissing,
            long totalRequests,
            long completedRequests,
            long delayedTasks,
            BigDecimal totalDistanceKm,
            long distanceSampleCount,
            BigDecimal totalEstimatedCost,
            BigDecimal totalActualCost) {}
}
