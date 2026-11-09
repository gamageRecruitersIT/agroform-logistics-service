package com.example.logistics.repository;

import com.example.logistics.dto.response.TransportReportData.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Read-only aggregate queries for the Monitoring & Reporting service.
 * Reporting window = transport_request.created_at in [from, to). Optional district filter
 * matches the normalised transport_request.pickup_district.
 * route_estimate and transport_task are both 1:1 with transport_request (unique constraints),
 * so the joins below never double-count.
 */
@Repository
@RequiredArgsConstructor
public class TransportReportQueryRepository {

    private final JdbcTemplate jdbc;

    private static String where(String district, List<Object> params, OffsetDateTime from, OffsetDateTime to) {
        params.add(from);
        params.add(to);
        String w = " WHERE tr.created_at >= ? AND tr.created_at < ? ";
        if (district != null && !district.isBlank()) {
            w += " AND tr.pickup_district = ? ";
            params.add(district);
        }
        return w;
    }

    public TransportSummary summary(Window win) {
        List<Object> p = new ArrayList<>();
        String w = where(win.district(), p, win.from(), win.to());
        return jdbc.queryForObject("""
                SELECT
                  COUNT(*) AS total_requests,
                  COUNT(*) FILTER (WHERE tr.request_status = 'PENDING')   AS pending,
                  COUNT(*) FILTER (WHERE tr.request_status = 'ACCEPTED')  AS accepted,
                  COUNT(*) FILTER (WHERE tr.request_status = 'REJECTED')  AS rejected,
                  COUNT(*) FILTER (WHERE tr.request_status = 'CANCELLED') AS cancelled,
                  COUNT(*) FILTER (WHERE tr.request_status = 'COMPLETED') AS completed,
                  COUNT(*) FILTER (WHERE tr.pickup_district IS NULL OR TRIM(tr.pickup_district) = '') AS no_district,
                  COUNT(tt.transport_task_id) AS total_tasks,
                  COUNT(*) FILTER (WHERE tt.task_status = 'ASSIGNED')    AS t_assigned,
                  COUNT(*) FILTER (WHERE tt.task_status = 'IN_PROGRESS') AS t_in_progress,
                  COUNT(*) FILTER (WHERE tt.task_status = 'COMPLETED')   AS t_completed,
                  COUNT(*) FILTER (WHERE tt.task_status = 'CANCELLED')   AS t_cancelled,
                  COALESCE(SUM(tr.estimated_distance_km), 0) AS dist_sum,
                  COUNT(tr.estimated_distance_km)            AS dist_n,
                  COALESCE(SUM(re.estimated_cost), 0)        AS est_sum,
                  COUNT(re.estimated_cost)                   AS est_n,
                  COALESCE(SUM(tt.actual_cost), 0)           AS act_sum,
                  COUNT(tt.actual_cost)                      AS act_n
                FROM transport_request tr
                LEFT JOIN route_estimate re ON re.transport_request_id = tr.transport_request_id
                LEFT JOIN transport_task tt ON tt.transport_request_id = tr.transport_request_id
                """ + w, (rs, i) -> new TransportSummary(win,
                rs.getLong("total_requests"), rs.getLong("pending"), rs.getLong("accepted"),
                rs.getLong("rejected"), rs.getLong("cancelled"), rs.getLong("completed"),
                rs.getLong("no_district"),
                rs.getLong("total_tasks"), rs.getLong("t_assigned"), rs.getLong("t_in_progress"),
                rs.getLong("t_completed"), rs.getLong("t_cancelled"),
                rs.getBigDecimal("dist_sum"), rs.getLong("dist_n"),
                rs.getBigDecimal("est_sum"), rs.getLong("est_n"),
                rs.getBigDecimal("act_sum"), rs.getLong("act_n")), p.toArray());
    }

    public DeliveryPerformance deliveryPerformance(Window win) {
        List<Object> p = new ArrayList<>();
        String w = where(win.district(), p, win.from(), win.to());
        return jdbc.queryForObject("""
                WITH t AS (
                  SELECT tt.transport_task_id, tt.assigned_at,
                         (SELECT MIN(tu.recorded_at) FROM tracking_update tu
                           WHERE tu.transport_task_id = tt.transport_task_id
                             AND tu.update_type = 'STATUS_CHANGE'
                             AND tu.new_status = 'UNLOADED_AT_WAREHOUSE') AS unloaded_at
                  FROM transport_task tt
                  JOIN transport_request tr ON tr.transport_request_id = tt.transport_request_id
                """ + w + """
                )
                SELECT COUNT(*) AS tasks,
                       COUNT(unloaded_at) AS unloaded,
                       AVG(EXTRACT(EPOCH FROM (unloaded_at - assigned_at)) / 60) AS avg_min,
                       MIN(EXTRACT(EPOCH FROM (unloaded_at - assigned_at)) / 60) AS min_min,
                       MAX(EXTRACT(EPOCH FROM (unloaded_at - assigned_at)) / 60) AS max_min
                FROM t
                """, (rs, i) -> new DeliveryPerformance(win,
                rs.getLong("tasks"), rs.getLong("unloaded"),
                scale(rs.getBigDecimal("avg_min")), scale(rs.getBigDecimal("min_min")),
                scale(rs.getBigDecimal("max_min")),
                false, 0L, 0L,
                "No source-provided timing baseline (expected delivery time) exists in Logistics; "
                        + "on-time rate must not be calculated. Tasks are NOT treated as on-time."),
                p.toArray());
    }

    public DelaySummary delaySummary(Window win) {
        List<Object> p = new ArrayList<>();
        String w = where(win.district(), p, win.from(), win.to());
        String base = """
                FROM tracking_update tu
                JOIN transport_task tt ON tt.transport_task_id = tu.transport_task_id
                JOIN transport_request tr ON tr.transport_request_id = tt.transport_request_id
                """ + w + " AND tu.update_type = 'DELAY_FLAGGED' ";

        long tasksInWindow = jdbc.queryForObject("""
                SELECT COUNT(*) FROM transport_task tt
                JOIN transport_request tr ON tr.transport_request_id = tt.transport_request_id
                """ + w, Long.class, p.toArray());

        // Each flag is paired with the first DELAY_RESOLVED after it. LOCATION pings and other
        // tracking rows are ignored, and tasks are counted DISTINCT.
        DelaySummary core = jdbc.queryForObject("""
                WITH f AS (
                  SELECT tu.transport_task_id, tu.recorded_at AS flagged_at,
                         (SELECT MIN(r.recorded_at) FROM tracking_update r
                           WHERE r.transport_task_id = tu.transport_task_id
                             AND r.update_type = 'DELAY_RESOLVED'
                             AND r.recorded_at > tu.recorded_at) AS resolved_at
                  """ + base + """
                )
                SELECT COUNT(DISTINCT transport_task_id) AS delayed_tasks,
                       COUNT(*) AS episodes,
                       COUNT(resolved_at) AS resolved,
                       AVG(EXTRACT(EPOCH FROM (resolved_at - flagged_at)) / 60) AS avg_min
                FROM f
                """, (rs, i) -> new DelaySummary(win, tasksInWindow,
                rs.getLong("delayed_tasks"), rs.getLong("episodes"), rs.getLong("resolved"),
                0L, scale(rs.getBigDecimal("avg_min")), List.of()), p.toArray());

        List<Object> p2 = new ArrayList<>();
        String w2 = where(win.district(), p2, win.from(), win.to());
        long currentlyDelayed = jdbc.queryForObject("""
                SELECT COUNT(*) FROM delivery_status ds
                JOIN transport_task tt ON tt.transport_task_id = ds.transport_task_id
                JOIN transport_request tr ON tr.transport_request_id = tt.transport_request_id
                """ + w2 + " AND ds.is_delayed = TRUE ", Long.class, p2.toArray());

        List<DelayReasonCount> reasons = jdbc.query("""
                SELECT COALESCE(NULLIF(TRIM(tu.delay_reason), ''), 'UNSPECIFIED') AS reason, COUNT(*) AS n
                """ + base + """
                GROUP BY 1 ORDER BY n DESC, reason ASC LIMIT 10
                """, (rs, i) -> new DelayReasonCount(rs.getString("reason"), rs.getLong("n")), p.toArray());

        return new DelaySummary(win, core.tasksInWindow(), core.delayedTasks(), core.delayEpisodes(),
                core.resolvedEpisodes(), currentlyDelayed, core.avgResolvedDelayMinutes(), reasons);
    }

    public List<DistrictBreakdown> districtBreakdown(OffsetDateTime from, OffsetDateTime to) {
        return jdbc.query("""
                SELECT COALESCE(NULLIF(TRIM(tr.pickup_district), ''), 'UNKNOWN') AS district,
                       (tr.pickup_district IS NULL OR TRIM(tr.pickup_district) = '') AS missing,
                       COUNT(*) AS total_requests,
                       COUNT(*) FILTER (WHERE tr.request_status = 'COMPLETED') AS completed,
                       COUNT(DISTINCT tt.transport_task_id) FILTER (WHERE EXISTS (
                           SELECT 1 FROM tracking_update tu
                            WHERE tu.transport_task_id = tt.transport_task_id
                              AND tu.update_type = 'DELAY_FLAGGED')) AS delayed_tasks,
                       COALESCE(SUM(tr.estimated_distance_km), 0) AS dist_sum,
                       COUNT(tr.estimated_distance_km) AS dist_n,
                       COALESCE(SUM(re.estimated_cost), 0) AS est_sum,
                       COALESCE(SUM(tt.actual_cost), 0) AS act_sum
                FROM transport_request tr
                LEFT JOIN route_estimate re ON re.transport_request_id = tr.transport_request_id
                LEFT JOIN transport_task tt ON tt.transport_request_id = tr.transport_request_id
                WHERE tr.created_at >= ? AND tr.created_at < ?
                GROUP BY 1, 2
                ORDER BY total_requests DESC, district ASC
                """, (rs, i) -> new DistrictBreakdown(rs.getString("district"), rs.getBoolean("missing"),
                rs.getLong("total_requests"), rs.getLong("completed"), rs.getLong("delayed_tasks"),
                rs.getBigDecimal("dist_sum"), rs.getLong("dist_n"),
                rs.getBigDecimal("est_sum"), rs.getBigDecimal("act_sum")), from, to);
    }

    private static BigDecimal scale(BigDecimal v) {
        return v == null ? null : v.setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
