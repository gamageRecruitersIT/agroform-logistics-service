package com.example.logistics.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Read-only lookups on transport_task (+ transport_request for the farmer id).
 * Uses plain SQL on purpose so the tracking module does not depend on the
 * TransportTask entity/repository that Chamuditha is still building.
 * If Chamuditha's repository is ready later, this class can delegate to it.
 */
@Repository
@RequiredArgsConstructor
public class TransportTaskLookupRepository {

    private static final String BASE_SQL = """
            SELECT t.transport_task_id, t.transport_task_code, t.transport_request_id,
                   t.transporter_id, t.driver_id, r.farmer_id
            FROM transport_task t
            JOIN transport_request r ON r.transport_request_id = t.transport_request_id
            """;

    private static final RowMapper<TransportTaskRef> MAPPER = (rs, i) -> new TransportTaskRef(
            rs.getObject("transport_task_id", UUID.class),
            rs.getString("transport_task_code"),
            rs.getObject("transport_request_id", UUID.class),
            rs.getObject("transporter_id", UUID.class),
            rs.getObject("driver_id", UUID.class),
            rs.getObject("farmer_id", UUID.class));

    private final JdbcTemplate jdbc;

    public Optional<TransportTaskRef> findByCode(String taskCode) {
        return jdbc.query(BASE_SQL + " WHERE t.transport_task_code = ?", MAPPER, taskCode)
                .stream().findFirst();
    }

    public Optional<TransportTaskRef> findById(UUID transportTaskId) {
        return jdbc.query(BASE_SQL + " WHERE t.transport_task_id = ?", MAPPER, transportTaskId)
                .stream().findFirst();
    }

    public List<TransportTaskRef> findByDriver(UUID driverId) {
        return jdbc.query(BASE_SQL + " WHERE t.driver_id = ? ORDER BY t.assigned_at DESC", MAPPER, driverId);
    }

    public List<TransportTaskRef> findByTransporter(UUID transporterId) {
        return jdbc.query(BASE_SQL + " WHERE t.transporter_id = ? ORDER BY t.assigned_at DESC", MAPPER, transporterId);
    }

    public List<TransportTaskRef> findByFarmer(UUID farmerId) {
        return jdbc.query(BASE_SQL + " WHERE r.farmer_id = ? ORDER BY t.assigned_at DESC", MAPPER, farmerId);
    }
}
