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
        UUID farmerId,
        String requestStatus) {

    /**
     * True when the parent transport request was cancelled or rejected, i.e. the
     * delivery must not move any further (owner: Dinujaya's request lifecycle).
     */
    public boolean isRequestClosed() {
        return "CANCELLED".equals(requestStatus) || "REJECTED".equals(requestStatus);
    }
}