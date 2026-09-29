package com.example.logistics.repository;

import java.util.UUID;

/**
 * Read-only view of a transport task, used by the tracking module to translate the
 * public transport_task_code into ids and to check who may access the task.
 * (The TransportTask entity itself belongs to Chamuditha's module.)
 */
public record TransportTaskRef(
        UUID transportTaskId,
        String transportTaskCode,
        UUID transportRequestId,
        UUID transporterId,
        UUID driverId,
        UUID farmerId) {
}
