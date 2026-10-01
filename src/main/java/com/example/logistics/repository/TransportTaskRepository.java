package com.example.logistics.repository;

import com.example.logistics.entity.TransportTask;
import com.example.logistics.entity.enums.TransportTaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransportTaskRepository extends JpaRepository<TransportTask, UUID> {

    /**
     * Find a task by its UI-friendly public code.
     */
    Optional<TransportTask> findByTransportTaskCode(String transportTaskCode);

    /**
     * Find a task linked to a specific transport request.
     * (One task per request — enforced by DB unique constraint.)
     */
    Optional<TransportTask> findByTransportRequestId(UUID transportRequestId);

    /**
     * Check if an active task already exists for the given transport request.
     * Used to prevent duplicate task creation.
     */
    boolean existsByTransportRequestId(UUID transportRequestId);

    /**
     * Get all tasks assigned to a specific transporter, ordered newest first.
     */
    List<TransportTask> findByTransporterIdOrderByAssignedAtDesc(UUID transporterId);

    /**
     * Get all tasks assigned to a specific driver, ordered newest first.
     * Used for driver task history.
     */
    List<TransportTask> findByDriverIdOrderByAssignedAtDesc(UUID driverId);

    /**
     * Get all tasks for a driver filtered by status.
     * Used to find current active task for a driver.
     */
    List<TransportTask> findByDriverIdAndTaskStatus(UUID driverId, TransportTaskStatus status);
}
